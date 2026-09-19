package com.example.inkpaperdiary.draft

import android.net.Uri
import com.example.inkpaperdiary.data.repository.DiaryStore
import com.example.inkpaperdiary.data.repository.DraftStore
import com.example.inkpaperdiary.data.repository.MediaStore
import com.example.inkpaperdiary.domain.model.Attachment
import com.example.inkpaperdiary.domain.model.Diary
import com.example.inkpaperdiary.domain.model.DiaryDraft
import com.example.inkpaperdiary.ui.editor.EditorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 6 异常场景矩阵：用户输入内容后遭遇各种异常，重开后内容必须全部找回。
 *
 * FakeDraftStore 在这里扮演"磁盘上的 Room drafts 表"：
 * 它的生命周期独立于任何 EditorViewModel 实例，等价于进程/Activity 销毁后数据仍在磁盘。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DraftPersistenceScenariosTest {

    private val scheduler = TestCoroutineScheduler()

    private class FakeDraftStore : DraftStore {
        val drafts = LinkedHashMap<String, DiaryDraft>()
        override suspend fun saveDraft(draft: DiaryDraft) { drafts[draft.draftId] = draft }
        override suspend fun getDraft(draftId: String): DiaryDraft? = drafts[draftId]
        override suspend fun getLatestNewDiaryDraft(): DiaryDraft? =
            drafts.values.filter { it.diaryId == null }.maxByOrNull { it.updatedTime }
        override suspend fun getDraftForDiary(diaryId: String): DiaryDraft? =
            drafts.values.filter { it.diaryId == diaryId }.maxByOrNull { it.updatedTime }
        override suspend fun getAllDrafts(): List<DiaryDraft> = drafts.values.toList()
        override suspend fun deleteDraft(draftId: String) { drafts.remove(draftId) }
        override suspend fun deleteDraftsForDiary(diaryId: String) { drafts.values.removeAll { it.diaryId == diaryId } }
        override suspend fun purgeEmptyDrafts(): Int {
            val empty = drafts.values.filter { it.isBlank }
            empty.forEach { drafts.remove(it.draftId) }
            return empty.size
        }
    }

    private class FakeDiaryStore : DiaryStore {
        val diaries = mutableMapOf<String, Diary>()
        override suspend fun getDiaryById(id: String): Diary? = diaries[id]
        override suspend fun saveDiary(diary: Diary) { diaries[diary.id] = diary }
    }

    private class FakeMediaStore : MediaStore {
        override suspend fun saveImageFromUri(uri: Uri, diaryId: String): Attachment? = null
    }

    private lateinit var diskStore: FakeDraftStore
    private lateinit var diaryStore: FakeDiaryStore
    private var now = 2_000_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        diskStore = FakeDraftStore()
        diaryStore = FakeDiaryStore()
        now = 2_000_000L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 模拟"重新打开 App"：新建编辑器实例，但磁盘草稿表保持原样 */
    private fun relaunch(diaryId: String? = null, initialEntryDate: Long? = null): EditorViewModel =
        EditorViewModel(
            diaryStore = diaryStore,
            draftStore = diskStore,
            mediaStore = FakeMediaStore(),
            diaryId = diaryId,
            initialEntryDate = initialEntryDate,
            autosaveScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(scheduler)),
            clock = { now }
        )

    private fun advance(ms: Long) {
        now += ms
        scheduler.advanceTimeBy(ms)
        scheduler.runCurrent()
    }

    /** 情况1：输入内容 → 返回 → 再进入（返回键只保存草稿，不打扰用户） */
    @Test
    fun `情况1_输入后返回再进入_内容完整找回`() {
        val vm = relaunch()
        scheduler.runCurrent()
        vm.updateTitle("情况一")
        vm.updateContent("写完这段就退出，回来必须还在。")
        vm.flushDraftNow()
        scheduler.runCurrent()

        val reopened = relaunch()
        scheduler.runCurrent()
        val pending = reopened.uiState.value.pendingDraft
        assertNotNull(pending)
        reopened.continuePendingDraft()
        assertEquals("写完这段就退出，回来必须还在。", reopened.uiState.value.contentMarkdown)
        assertEquals("情况一", reopened.uiState.value.title)
    }

    /** 情况2：输入内容 → 杀掉 App → 重启（进程死亡，防抖自动保存兜底） */
    @Test
    fun `情况2_进程死亡后重启_内容完整找回`() {
        val vm = relaunch()
        scheduler.runCurrent()
        vm.updateContent("App 被系统杀死前写下的最后一段话。")
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS) // 防抖窗口过后被杀
        assertTrue(diskStore.drafts.isNotEmpty())

        val reopened = relaunch()
        scheduler.runCurrent()
        assertNotNull(reopened.uiState.value.pendingDraft)
        reopened.continuePendingDraft()
        assertEquals("App 被系统杀死前写下的最后一段话。", reopened.uiState.value.contentMarkdown)
    }

    /** 情况3：输入内容 → 手机重启（磁盘持久数据在重启后仍在，等价于草稿表持久） */
    @Test
    fun `情况3_手机重启后_内容完整找回`() {
        val vm = relaunch()
        scheduler.runCurrent()
        vm.updateTitle("重启也不丢")
        vm.updateContent("手机重启后草稿仍在磁盘上。")
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS)
        val savedDraft = diskStore.drafts.values.single()

        // "重启"：草稿表数据原样保留（Room 落盘于应用数据目录）
        val reopened = relaunch()
        scheduler.runCurrent()
        assertEquals(savedDraft, reopened.uiState.value.pendingDraft)
        reopened.continuePendingDraft()
        assertEquals("重启也不丢", reopened.uiState.value.title)
        assertEquals("手机重启后草稿仍在磁盘上。", reopened.uiState.value.contentMarkdown)
    }

    /** 情况4：输入内容 → 切换深色模式（Activity 重建，编辑器实例全部重建） */
    @Test
    fun `情况4_切换深色模式Activity重建_内容完整找回`() {
        val vm = relaunch()
        scheduler.runCurrent()
        vm.updateContent("深色模式切换导致重建。")
        vm.flushDraftNow() // 重建前的 onStop 兜底落盘
        scheduler.runCurrent()

        val recreated = relaunch()
        scheduler.runCurrent()
        assertNotNull(recreated.uiState.value.pendingDraft)
        recreated.continuePendingDraft()
        assertEquals("深色模式切换导致重建。", recreated.uiState.value.contentMarkdown)
    }

    /** 情况5：输入大量文字（防抖落盘与恢复都必须无损） */
    @Test
    fun `情况5_大量文字输入_内容无损`() {
        val largeContent = buildString {
            repeat(2000) { i -> append("第${i}行：山有木兮木有枝，心悦君兮君不知。\n") }
        }
        assertTrue("测试自检：内容应超过 100KB", largeContent.toByteArray().size > 100_000)

        val vm = relaunch()
        scheduler.runCurrent()
        vm.updateContent(largeContent)
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS)

        val reopened = relaunch()
        scheduler.runCurrent()
        reopened.continuePendingDraft()
        assertEquals(largeContent, reopened.uiState.value.contentMarkdown)
    }

    /** 情况6：编辑图片（插图随草稿落盘、可移除、可恢复） */
    @Test
    fun `情况6_编辑图片后异常退出_插图随草稿找回`() {
        val attachment = Attachment(
            id = "img-1", diaryId = "session-1",
            localPath = "/data/diary_media/img_1.webp", fileName = "img_1.webp", fileSize = 2048
        )
        diskStore.drafts["session-1"] = DiaryDraft(
            draftId = "session-1", diaryId = null, userId = "local_guest",
            title = "带插图的一页", contentMarkdown = "正文",
            entryDate = now, createdTime = now, updatedTime = now,
            attachments = listOf(attachment)
        )

        val reopened = relaunch()
        scheduler.runCurrent()
        assertNotNull(reopened.uiState.value.pendingDraft)
        reopened.continuePendingDraft()
        assertEquals(listOf(attachment), reopened.uiState.value.attachments)

        // 继续会话中移除插图 → 草稿同步更新
        reopened.removeAttachment("img-1")
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS)
        assertTrue(diskStore.drafts.getValue("session-1").attachments.isEmpty())
        assertEquals(0, reopened.uiState.value.attachments.size)
    }

    /** 编辑既有日记时的异常退出：草稿遮蔽正式数据，但绝不覆盖，用户可选还原 */
    @Test
    fun `编辑既有日记异常退出_草稿与正式数据互不污染`() {
        val diaryId = "diary-x"
        diaryStore.diaries[diaryId] = Diary(
            id = diaryId, title = "旧标题", contentMarkdown = "旧内容",
            entryDate = 100L, createdAt = 100L, updatedAt = 100L
        )

        val vm = relaunch(diaryId = diaryId)
        scheduler.runCurrent()
        vm.updateTitle("新标题")
        vm.updateContent("新内容")
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS)

        // 正式日记完全未被改动
        assertEquals("旧标题", diaryStore.diaries.getValue(diaryId).title)
        assertEquals("旧内容", diaryStore.diaries.getValue(diaryId).contentMarkdown)

        val reopened = relaunch(diaryId = diaryId)
        scheduler.runCurrent()
        val pending = reopened.uiState.value.pendingDraft
        assertNotNull(pending)
        assertEquals("新内容", pending!!.contentMarkdown)
        assertEquals("旧标题", reopened.uiState.value.title) // 背后仍是正式内容
        reopened.continuePendingDraft()
        assertEquals("新标题", reopened.uiState.value.title)

        // 完成转正：正式数据更新，草稿清除
        reopened.finishDiary { }
        scheduler.runCurrent()
        assertEquals("新标题", diaryStore.diaries.getValue(diaryId).title)
        assertTrue(diskStore.drafts.isEmpty())
    }

    /** 恢复提示被"暂不处理"后再次进入，草稿仍在（不丢、不强制） */
    @Test
    fun `暂不处理后再次进入_草稿仍在可恢复`() {
        val vm1 = relaunch()
        scheduler.runCurrent()
        vm1.updateContent("第一次进入写下")
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS)

        val vm2 = relaunch()
        scheduler.runCurrent()
        vm2.dismissPendingDraftPrompt() // 用户暂不处理
        assertNull(vm2.uiState.value.pendingDraft)

        val vm3 = relaunch()
        scheduler.runCurrent()
        assertNotNull("草稿应保留", vm3.uiState.value.pendingDraft)
        vm3.continuePendingDraft()
        assertEquals("第一次进入写下", vm3.uiState.value.contentMarkdown)
    }
}
