package com.example.inkpaperdiary.ui.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inkpaperdiary.data.repository.DiaryStore
import com.example.inkpaperdiary.data.repository.DraftStore
import com.example.inkpaperdiary.data.repository.MediaStore
import com.example.inkpaperdiary.domain.model.Attachment
import com.example.inkpaperdiary.domain.model.Diary
import com.example.inkpaperdiary.domain.model.DiaryDraft
import com.example.inkpaperdiary.domain.model.DraftMetadata
import com.example.inkpaperdiary.domain.model.Tag
import com.example.inkpaperdiary.domain.model.Weather
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.*

data class EditorUiState(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "local_guest",
    val title: String = "",
    val contentMarkdown: String = "",
    val locationName: String? = null,
    val entryDate: Long = System.currentTimeMillis(),
    val createdAt: Long = 0L,
    val tags: List<Tag> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val isPinned: Boolean = false,
    val isLoaded: Boolean = false,
    /** 自动保存：最近一次草稿落库是否进行中。 */
    val isSaving: Boolean = false,
    /** 自动保存：最近一次成功落库时间；0 表示从未保存。 */
    val lastSavedAt: Long = 0L,
    /** 待用户决定的恢复草稿（进入编辑器时检测到未完成的草稿，弹恢复提示） */
    val pendingDraft: DiaryDraft? = null
)

/**
 * 参与草稿持久化的字段投影；用 data class 相等性做快照去重，
 * 避免 flush 与防抖先后触发造成的重复落盘。
 */
private data class DraftPayload(
    val title: String,
    val contentMarkdown: String,
    val locationName: String?,
    val entryDate: Long,
    val isPinned: Boolean,
    val tags: List<Tag>,
    val attachments: List<Attachment>
)

private fun EditorUiState.toDraftPayload() = DraftPayload(
    title = title,
    contentMarkdown = contentMarkdown,
    locationName = locationName,
    entryDate = entryDate,
    isPinned = isPinned,
    tags = tags,
    attachments = attachments
)

/** 空内容判定：空草稿没有保存价值（保存空白不产生任何记录） */
private fun DraftPayload.isBlank() =
    title.isBlank() &&
        contentMarkdown.isBlank() &&
        attachments.isEmpty() &&
        tags.isEmpty() &&
        locationName.isNullOrBlank()

/** 草稿 → 编辑器状态（恢复草稿时使用） */
private fun DiaryDraft.toEditorState(): EditorUiState = EditorUiState(
    id = draftId,
    userId = userId,
    title = title,
    contentMarkdown = contentMarkdown,
    locationName = locationName,
    entryDate = entryDate,
    createdAt = diaryCreatedAt,
    tags = tags,
    attachments = attachments,
    isPinned = isPinned,
    isLoaded = true
)

/**
 * 数字纸张编辑器 ViewModel。
 *
 * 草稿持久化系统：写作即保存，离开即入册。
 * - 落笔后防抖自动写入 Room `drafts` 表（进程死亡/崩溃的安全网）；
 * - 顶栏呈现一行安静的保存状态；
 * - 任何离开编辑器的方式（返回键、"完成"、切后台）都会把非空白内容立即转正进正式日记，
 *   用户回到时间线/阅读页立刻能看到，绝不出现"写了却不在"；
 * - 只有进程死亡/崩溃等异常导致转正没来得及发生时，草稿才会留存，
 *   重新进入编辑器时弹出恢复提示，把内容交还给用户决定。
 */
@OptIn(FlowPreview::class)
class EditorViewModel(
    private val diaryStore: DiaryStore,
    private val draftStore: DraftStore,
    private val mediaStore: MediaStore,
    private val diaryId: String?,
    private val initialEntryDate: Long? = null,
    /**
     * 应用级协程作用域：自动保存引擎跑在这里而不是 viewModelScope。
     * 即使 ViewModel 随导航销毁/进程死亡前的窗口期，最后一次防抖写入仍能完成落盘。
     */
    private val autosaveScope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    /** 每次编辑递增；防抖后触发草稿落盘。 */
    private val editSignal = MutableStateFlow(0L)
    private val persistMutex = Mutex()

    private var lastPersistedPayload: DraftPayload? = null
    private var lastPromotedPayload: DraftPayload? = null
    private var draftCreatedAt = 0L
    private var draftAlive = false      // 草稿表中是否存在本会话的草稿
    private var sessionClosed = false   // 离开编辑器之后关闭会话，禁止再写草稿

    /** 是否为"新建日记"会话（diaryId 为空或 "new"） */
    private val isNewSession = diaryId == null || diaryId == "new"

    init {
        loadDiary()
        autosaveScope.launch {
            editSignal
                .debounce(DRAFT_AUTOSAVE_DELAY_MS)
                .collectLatest { persistDraft() }
        }
    }

    // ------------------------------------------------------------------
    // 初始化与草稿恢复
    // ------------------------------------------------------------------

    private fun loadDiary() {
        if (isNewSession) {
            viewModelScope.launch {
                // 打开编辑器即清理历史空草稿（空草稿没有恢复价值）
                draftStore.purgeEmptyDrafts()
                val draft = draftStore.getLatestNewDiaryDraft()?.takeIf { !it.isBlank }
                _uiState.update {
                    it.copy(
                        isLoaded = true,
                        // 支持"补写某天"：新建日记时用目标日期而不是当前时间
                        entryDate = initialEntryDate ?: clock(),
                        pendingDraft = draft
                    )
                }
            }
            return
        }
        viewModelScope.launch {
            val existing = diaryStore.getDiaryById(diaryId!!)
            val base = if (existing != null) {
                EditorUiState(
                    id = existing.id,
                    userId = existing.userId,
                    title = existing.title,
                    contentMarkdown = existing.contentMarkdown,
                    locationName = existing.locationName,
                    entryDate = existing.entryDate,
                    createdAt = existing.createdAt,
                    tags = existing.tags,
                    attachments = existing.attachments,
                    isPinned = existing.isPinned,
                    isLoaded = true
                )
            } else {
                EditorUiState(isLoaded = true)
            }

            // 既有日记存在更新的编辑草稿时给出恢复提示，绝不静默覆盖正式内容
            val draft = draftStore.getDraftForDiary(diaryId)
            val recoverable = draft?.takeIf {
                !it.isBlank && (existing == null || it.updatedTime > existing.updatedAt)
            }
            if (draft != null && recoverable == null) {
                // 草稿比正式数据旧（过期残留），直接清理
                draftStore.deleteDraft(draft.draftId)
            }
            _uiState.value = base.copy(pendingDraft = recoverable)
        }
    }

    /** 恢复提示 - 继续编辑：以草稿内容接管编辑器 */
    fun continuePendingDraft() {
        val draft = _uiState.value.pendingDraft ?: return
        val restored = draft.toEditorState()
        _uiState.value = restored.copy(lastSavedAt = draft.updatedTime)
        draftAlive = true
        draftCreatedAt = draft.createdTime
        lastPersistedPayload = restored.toDraftPayload()
        lastPromotedPayload = null
    }

    /** 恢复提示 - 删除草稿（新建场景）：草稿与其孤儿配图一并清理，编辑器保持空白 */
    fun discardPendingDraft() {
        val draft = _uiState.value.pendingDraft ?: return
        autosaveScope.launch {
            draftStore.deleteDraft(draft.draftId)
            if (draft.diaryId == null) {
                draft.attachments.forEach { runCatching { File(it.localPath).delete() } }
            }
        }
        _uiState.update { it.copy(pendingDraft = null) }
    }

    /** 恢复提示 - 还原为已保存版本（编辑既有日记场景）：丢弃草稿，回到正式日记内容 */
    fun revertPendingDraftToDiary() {
        val draft = _uiState.value.pendingDraft ?: return
        autosaveScope.launch { draftStore.deleteDraft(draft.draftId) }
        _uiState.update { it.copy(pendingDraft = null) }
    }

    /** 恢复提示 - 暂不处理：只收起提示，草稿保留（下次进入仍可恢复） */
    fun dismissPendingDraftPrompt() {
        _uiState.update { it.copy(pendingDraft = null) }
    }

    // ------------------------------------------------------------------
    // 用户输入：更新状态并触发防抖自动保存
    // ------------------------------------------------------------------

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
        notifyEdited()
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(contentMarkdown = content) }
        notifyEdited()
    }

    fun updateLocation(location: String?) {
        _uiState.update { it.copy(locationName = location) }
        notifyEdited()
    }

    fun updateEntryDate(timestamp: Long) {
        _uiState.update { it.copy(entryDate = timestamp) }
        notifyEdited()
    }

    fun togglePinned() {
        _uiState.update { it.copy(isPinned = !it.isPinned) }
        notifyEdited()
    }

    fun addTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val currentTags = _uiState.value.tags
        if (currentTags.any { it.name.equals(trimmed, ignoreCase = true) }) return
        val newTag = Tag(id = UUID.randomUUID().toString(), name = trimmed)
        _uiState.update { it.copy(tags = currentTags + newTag) }
        notifyEdited()
    }

    fun removeTag(tagId: String) {
        _uiState.update {
            it.copy(tags = it.tags.filterNot { tag -> tag.id == tagId })
        }
        notifyEdited()
    }

    fun addImage(uri: Uri) {
        viewModelScope.launch {
            val attachment = mediaStore.saveImageFromUri(uri, _uiState.value.id)
            if (attachment != null) {
                _uiState.update { it.copy(attachments = it.attachments + attachment) }
                notifyEdited()
            }
        }
    }

    fun removeAttachment(attachmentId: String) {
        _uiState.update {
            it.copy(attachments = it.attachments.filterNot { att -> att.id == attachmentId })
        }
        notifyEdited()
    }

    private fun notifyEdited() {
        if (sessionClosed) return
        editSignal.value += 1
    }

    // ------------------------------------------------------------------
    // 草稿落盘
    // ------------------------------------------------------------------

    private suspend fun persistDraft() {
        if (sessionClosed) return
        persistMutex.withLock {
            val state = _uiState.value
            if (!state.isLoaded || state.pendingDraft != null) return@withLock
            val payload = state.toDraftPayload()

            if (payload.isBlank()) {
                // 内容被清空：删除草稿而不是保存空草稿
                if (draftAlive) {
                    draftStore.deleteDraft(state.id)
                    draftAlive = false
                    draftCreatedAt = 0L
                }
                lastPersistedPayload = null
                _uiState.update { it.copy(isSaving = false, lastSavedAt = 0L) }
                return@withLock
            }

            if (draftAlive && payload == lastPersistedPayload) {
                // 内容与已落盘草稿一致（flush 与防抖先后触发），无需重写
                return@withLock
            }

            if (!draftAlive && payload == lastPromotedPayload) {
                // 内容已转正进正式日记且无新改动：不要为已入册的内容重建草稿
                return@withLock
            }

            _uiState.update { it.copy(isSaving = true) }
            val now = clock()
            if (draftCreatedAt == 0L) draftCreatedAt = now
            draftStore.saveDraft(
                DiaryDraft(
                    draftId = state.id,
                    diaryId = if (isNewSession) null else state.id,
                    userId = state.userId,
                    title = state.title,
                    contentMarkdown = state.contentMarkdown,
                    weather = Weather.SUNNY, // 天气不再主动采集；历史数据保留
                    locationName = state.locationName,
                    entryDate = state.entryDate,
                    diaryCreatedAt = state.createdAt,
                    isPinned = state.isPinned,
                    tags = state.tags,
                    attachments = state.attachments,
                    createdTime = draftCreatedAt,
                    updatedTime = now,
                    metadata = DraftMetadata(initialEntryDate = initialEntryDate)
                )
            )
            draftAlive = true
            lastPersistedPayload = payload
            _uiState.update { it.copy(isSaving = false, lastSavedAt = now) }
        }
    }

    /**
     * 立即转正（不等防抖、不关闭会话）：进入后台（onStop）等生命周期关口调用。
     * 非空白内容立刻写进正式日记——即使用户随后被系统杀掉进程，回到应用也能在时间线看到；
     * 会话保持打开，用户返回后还能继续写，后续编辑会以同一 id 增量更新。
     * 运行在 autosaveScope，即使 ViewModel 随导航销毁，写入仍会完成。
     */
    fun promoteNow() {
        if (sessionClosed) return
        autosaveScope.launch { promoteLocked(closeSession = false) }
    }

    // ------------------------------------------------------------------
    // 转正（完成 / 离开）/ 放弃
    // ------------------------------------------------------------------

    /**
     * 离开编辑器（返回键、"完成"）：转正并关闭会话。
     * 非空白内容写入正式日记并清除草稿；空白页直接退出、不产生记录。
     */
    fun finishDiary(onFinished: () -> Unit) {
        viewModelScope.launch {
            promoteLocked(closeSession = true)
            onFinished()
        }
    }

    /** 转正内核：非空白内容写入正式日记并清除草稿；空白页清理残留草稿、不产生记录。 */
    private suspend fun promoteLocked(closeSession: Boolean) {
        persistMutex.withLock {
            if (closeSession) sessionClosed = true
            val state = _uiState.value
            if (!state.isLoaded || state.pendingDraft != null) return@withLock
            val payload = state.toDraftPayload()

            if (payload.isBlank()) {
                // 空白页：清理会话草稿后结束，不产生任何记录
                if (draftAlive) {
                    draftStore.deleteDraft(state.id)
                    draftAlive = false
                }
                lastPersistedPayload = null
                _uiState.update { it.copy(isSaving = false, lastSavedAt = 0L) }
                return@withLock
            }

            if (!closeSession && payload == lastPromotedPayload && !draftAlive) {
                // 会话继续时无新改动，且内容已在正式日记中
                return@withLock
            }

            _uiState.update { it.copy(isSaving = true) }
            val now = clock()
            diaryStore.saveDiary(state.toDiary())
            // 正式数据已落库，草稿使命完成
            draftStore.deleteDraft(state.id)
            draftAlive = false
            lastPersistedPayload = null
            lastPromotedPayload = payload
            _uiState.update {
                it.copy(
                    isSaving = false,
                    lastSavedAt = now,
                    // 首次落库后记录 createdAt，后续转正不再重置创建时间
                    createdAt = if (it.createdAt == 0L) it.entryDate else it.createdAt
                )
            }
        }
    }

    private fun EditorUiState.toDiary() = Diary(
        id = id,
        userId = userId,
        title = title.trim(),
        contentMarkdown = contentMarkdown,
        weather = Weather.SUNNY, // 天气不再主动采集；历史数据保留
        locationName = locationName?.trim(),
        entryDate = entryDate,
        createdAt = createdAt, // 保留原创建时间，编辑不重置
        tags = tags,
        attachments = attachments,
        isPinned = isPinned
    )

    companion object {
        /** 自动保存防抖窗口：停止输入 800ms 后落盘（快到几乎不惧进程死亡，慢到不卡输入）。 */
        const val DRAFT_AUTOSAVE_DELAY_MS = 800L
    }
}
