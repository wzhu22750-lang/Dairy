package com.example.inkpaperdiary.draft

import android.net.Uri
import com.example.inkpaperdiary.data.repository.DiaryStore
import com.example.inkpaperdiary.data.repository.DraftStore
import com.example.inkpaperdiary.data.repository.MediaStore
import com.example.inkpaperdiary.domain.model.Attachment
import com.example.inkpaperdiary.domain.model.Diary
import com.example.inkpaperdiary.domain.model.DiaryDraft
import com.example.inkpaperdiary.domain.model.DraftMetadata
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

/**
 * 草稿持久化核心行为测试（JVM，虚拟时间驱动防抖）。
 *
 * FakeDraftStore 以内存模拟 Room drafts 表的持久语义：
 * ViewModel 实例销毁后数据仍在，等价于"进程死亡后磁盘仍在"。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelDraftTest {

    private val scheduler = TestCoroutineScheduler()

    /** 模拟磁盘上的 drafts 表：跨 ViewModel 实例（进程）持久 */
    private class FakeDraftStore : DraftStore {
        val drafts = LinkedHashMap<String, DiaryDraft>()
        var saveCount = 0

        override suspend fun saveDraft(draft: DiaryDraft) {
            saveCount++
            drafts[draft.draftId] = draft
        }

        override suspend fun getDraft(draftId: String): DiaryDraft? = drafts[draftId]

        override suspend fun getLatestNewDiaryDraft(): DiaryDraft? =
            drafts.values.filter { it.diaryId == null }.maxByOrNull { it.updatedTime }

        override suspend fun getDraftForDiary(diaryId: String): DiaryDraft? =
            drafts.values.filter { it.diaryId == diaryId }.maxByOrNull { it.updatedTime }

        override suspend fun getAllDrafts(): List<DiaryDraft> = drafts.values.toList()

        override suspend fun deleteDraft(draftId: String) {
            drafts.remove(draftId)
        }

        override suspend fun deleteDraftsForDiary(diaryId: String) {
            drafts.values.removeAll { it.diaryId == diaryId }
        }

        override suspend fun purgeEmptyDrafts(): Int {
            val empty = drafts.values.filter { it.isBlank }
            empty.forEach { drafts.remove(it.draftId) }
            return empty.size
        }
    }

    private class FakeDiaryStore : DiaryStore {
        val diaries = mutableMapOf<String, Diary>()
        override suspend fun getDiaryById(id: String): Diary? = diaries[id]
        override suspend fun saveDiary(diary: Diary) {
            diaries[diary.id] = diary
        }
    }

    private class FakeMediaStore : MediaStore {
        override suspend fun saveImageFromUri(uri: Uri, diaryId: String): Attachment? = null
    }

    private lateinit var draftStore: FakeDraftStore
    private lateinit var diaryStore: FakeDiaryStore
    private var now = 1_000_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        draftStore = FakeDraftStore()
        diaryStore = FakeDiaryStore()
        now = 1_000_000L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun clock(): Long = now

    private fun createViewModel(diaryId: String? = null, initialEntryDate: Long? = null): EditorViewModel =
        EditorViewModel(
            diaryStore = diaryStore,
            draftStore = draftStore,
            mediaStore = FakeMediaStore(),
            diaryId = diaryId,
            initialEntryDate = initialEntryDate,
            autosaveScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(scheduler)),
            clock = ::clock
        )

    private fun advance(ms: Long) {
        now += ms
        scheduler.advanceTimeBy(ms)
        scheduler.runCurrent()
    }

    private fun newDraft(
        draftId: String,
        diaryId: String?,
        title: String = "t$draftId",
        content: String = "c$draftId",
        updatedTime: Long
    ) = DiaryDraft(
        draftId = draftId,
        diaryId = diaryId,
        userId = "local_guest",
        title = title,
        contentMarkdown = content,
        entryDate = updatedTime,
        diaryCreatedAt = 0L,
        createdTime = updatedTime,
        updatedTime = updatedTime
    )

    // ------------------------------------------------------------------
    // 1. 输入文字后自动保存（防抖）
    // ------------------------------------------------------------------

    @Test
    fun `输入停止超过防抖窗口后草稿自动落盘`() {
        val vm = createViewModel()
        scheduler.runCurrent()

        vm.updateTitle("海边的一天")
        vm.updateContent("今天去了海边，风很大。")
        scheduler.runCurrent()
        assertTrue("防抖窗口内不应落盘", draftStore.drafts.isEmpty())

        advance(300)
        assertTrue("防抖窗口内不应落盘", draftStore.drafts.isEmpty())

        advance(600) // 累计越过 800ms
        assertEquals(1, draftStore.drafts.size)
        val draft = draftStore.drafts.values.single()
        assertEquals("海边的一天", draft.title)
        assertEquals("今天去了海边，风很大。", draft.contentMarkdown)
        assertNull("新建会话的草稿不应绑定正式日记", draft.diaryId)
        assertEquals(now, draft.updatedTime)
        assertEquals(now, vm.uiState.value.lastSavedAt)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `连续快速输入只触发一次落盘`() {
        val vm = createViewModel()
        scheduler.runCurrent()

        repeat(50) { i -> vm.updateContent("第 $i 行内容\n") }
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS)

        assertEquals("防抖后只应有一次写入", 1, draftStore.saveCount)
        assertEquals("第 49 行内容\n", draftStore.drafts.values.single().contentMarkdown)
    }

    @Test
    fun `promoteNow 不等防抖立即转正进正式日记`() {
        val vm = createViewModel()
        scheduler.runCurrent()

        vm.updateContent("来不及等防抖的内容")
        scheduler.runCurrent()
        assertTrue(draftStore.drafts.isEmpty())

        vm.promoteNow()
        scheduler.runCurrent()
        assertEquals("来不及等防抖的内容", diaryStore.diaries.values.single().contentMarkdown)
        assertTrue("转正后草稿应清除", draftStore.drafts.isEmpty())

        // 转正后防抖到点，不应为已入册的内容重建草稿
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS)
        assertTrue(draftStore.drafts.isEmpty())

        // 继续写作：会话仍打开，同一 id 增量更新，不产生第二篇
        vm.updateContent("来不及等防抖的内容\n追加一段")
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS)
        assertEquals(1, draftStore.drafts.size)

        vm.finishDiary { }
        scheduler.runCurrent()
        assertEquals(1, diaryStore.diaries.size)
        assertEquals("来不及等防抖的内容\n追加一段", diaryStore.diaries.values.single().contentMarkdown)
    }

    // ------------------------------------------------------------------
    // 2. 退出页面恢复 / 3. 进程死亡恢复（同机制：草稿表在会话之外持久）
    // ------------------------------------------------------------------

    @Test
    fun `返回键离开后内容立即转正进正式日记`() {
        val vm1 = createViewModel()
        scheduler.runCurrent()
        vm1.updateTitle("旅行日记")
        vm1.updateContent("第一天：抵达大理。")
        var finished = false
        vm1.finishDiary { finished = true } // 返回键路径：离开即转正
        scheduler.runCurrent()

        assertTrue(finished)
        val diary = diaryStore.diaries.values.single()
        assertEquals("旅行日记", diary.title)
        assertEquals("第一天：抵达大理。", diary.contentMarkdown)
        assertTrue("转正后不应残留草稿", draftStore.drafts.isEmpty())
    }

    @Test
    fun `进程死亡后重启仍能从草稿恢复`() {
        val vm1 = createViewModel()
        scheduler.runCurrent()
        vm1.updateContent("进程被杀前写下的文字")
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS) // 防抖自动落盘
        assertTrue(draftStore.drafts.isNotEmpty())

        // 模拟进程死亡后重启：全新 ViewModel，同一"磁盘"
        val vm2 = createViewModel()
        scheduler.runCurrent()

        val pending = vm2.uiState.value.pendingDraft
        assertNotNull(pending)
        assertEquals("进程被杀前写下的文字", pending!!.contentMarkdown)
        vm2.continuePendingDraft()
        assertEquals("进程被杀前写下的文字", vm2.uiState.value.contentMarkdown)
    }

    @Test
    fun `Activity销毁重建前内容已通过onStop转正`() {
        val vm1 = createViewModel()
        scheduler.runCurrent()
        vm1.updateTitle("配置变更")
        vm1.updateContent("切换深色模式导致 Activity 重建")
        vm1.promoteNow() // onStop 兜底转正
        scheduler.runCurrent()

        // 深色模式重建：内容已在正式日记中，不会丢失
        assertEquals("配置变更", diaryStore.diaries.values.single().title)
        assertTrue(draftStore.drafts.isEmpty())

        // 用户重新打开同一篇：编辑器直接载入已保存内容
        val diaryId = diaryStore.diaries.keys.single()
        val vm2 = createViewModel(diaryId = diaryId)
        scheduler.runCurrent()
        assertNull("无崩溃草稿时不应弹恢复提示", vm2.uiState.value.pendingDraft)
        assertEquals("切换深色模式导致 Activity 重建", vm2.uiState.value.contentMarkdown)
    }

    // ------------------------------------------------------------------
    // 4. 显式转正 / 还原 / 暂不处理
    // ------------------------------------------------------------------

    @Test
    fun `完成转正后草稿清除且正式日记落库`() {
        val vm = createViewModel()
        scheduler.runCurrent()
        vm.updateTitle("完成的一页")
        vm.updateContent("正文内容")
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS)
        assertEquals(1, draftStore.drafts.size)

        var finished = false
        vm.finishDiary { finished = true }
        scheduler.runCurrent()

        assertTrue(finished)
        assertTrue("转正后草稿应被清除", draftStore.drafts.isEmpty())
        val diary = diaryStore.diaries.values.single()
        assertEquals("完成的一页", diary.title)
        assertEquals("正文内容", diary.contentMarkdown)
    }

    @Test
    fun `空白日记完成时直接退出不产生记录`() {
        val vm = createViewModel()
        scheduler.runCurrent()

        var finished = false
        vm.finishDiary { finished = true }
        scheduler.runCurrent()

        assertTrue(finished)
        assertTrue(draftStore.drafts.isEmpty())
        assertTrue(diaryStore.diaries.isEmpty())
    }

    @Test
    fun `编辑既有日记时还原为已保存版本会删除草稿且不动正式数据`() {
        val diaryId = "diary-1"
        diaryStore.diaries[diaryId] = Diary(
            id = diaryId, title = "正式版本", contentMarkdown = "已保存的内容",
            entryDate = 500L, createdAt = 500L, updatedAt = 500L
        )
        draftStore.drafts[diaryId] = newDraft(diaryId, diaryId, "未保存编辑", "新写的内容", updatedTime = 600L)

        val vm = createViewModel(diaryId = diaryId)
        scheduler.runCurrent()
        assertNotNull(vm.uiState.value.pendingDraft)

        vm.revertPendingDraftToDiary()
        scheduler.runCurrent()
        assertNull(vm.uiState.value.pendingDraft)
        assertTrue("草稿应被删除", draftStore.drafts.isEmpty())
        assertEquals("已保存的内容", diaryStore.diaries.getValue(diaryId).contentMarkdown)

        // 编辑器回到正式日记内容
        assertEquals("正式版本", vm.uiState.value.title)
    }

    @Test
    fun `暂不处理只收起提示且草稿保留`() {
        draftStore.drafts["d1"] = newDraft("d1", null, updatedTime = 100L)
        val vm = createViewModel()
        scheduler.runCurrent()
        assertNotNull(vm.uiState.value.pendingDraft)

        vm.dismissPendingDraftPrompt()
        assertNull(vm.uiState.value.pendingDraft)
        assertEquals(1, draftStore.drafts.size)
    }

    // ------------------------------------------------------------------
    // 5. 空草稿清理
    // ------------------------------------------------------------------

    @Test
    fun `清空全部内容后草稿被删除而不是保存空草稿`() {
        val vm = createViewModel()
        scheduler.runCurrent()
        vm.updateContent("即将被清空的内容")
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS)
        assertEquals(1, draftStore.drafts.size)

        vm.updateContent("")
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS)
        assertTrue("空草稿不应存在", draftStore.drafts.isEmpty())
    }

    @Test
    fun `打开编辑器时清理历史空草稿`() {
        draftStore.drafts["empty"] = newDraft("empty", null, title = "", content = "", updatedTime = 1L)
        draftStore.drafts["real"] = newDraft("real", null, title = "真草稿", content = "有内容", updatedTime = 2L)

        createViewModel()
        scheduler.runCurrent()

        assertFalse(draftStore.drafts.containsKey("empty"))
        assertTrue(draftStore.drafts.containsKey("real"))
    }

    // ------------------------------------------------------------------
    // 6. 多个草稿管理
    // ------------------------------------------------------------------

    @Test
    fun `多个草稿共存且恢复最新的一个`() {
        draftStore.drafts["old"] = newDraft("old", null, "旧草稿", "旧内容", updatedTime = 100L)
        draftStore.drafts["new"] = newDraft("new", null, "新草稿", "新内容", updatedTime = 200L)

        val vm = createViewModel()
        scheduler.runCurrent()

        assertEquals("new", vm.uiState.value.pendingDraft?.draftId)
        vm.continuePendingDraft()
        assertEquals("新内容", vm.uiState.value.contentMarkdown)
        // 未被选择的草稿保留在库中（多个草稿共存）
        assertEquals(2, draftStore.drafts.size)
    }

    @Test
    fun `编辑既有日记的草稿与新日记草稿互不干扰`() = kotlinx.coroutines.runBlocking {
        draftStore.drafts["new-draft"] = newDraft("new-draft", null, updatedTime = 100L)
        draftStore.drafts["edit-draft"] = newDraft("edit-draft", "diary-9", updatedTime = 100L)

        assertEquals("new-draft", draftStore.getLatestNewDiaryDraft()!!.draftId)
        assertEquals("edit-draft", draftStore.getDraftForDiary("diary-9")!!.draftId)
        assertNull(draftStore.getDraftForDiary("diary-other"))
    }

    // ------------------------------------------------------------------
    // 7. 新建会话恢复后的延续编辑（沿用原 draftId，转正不产生第二篇）
    // ------------------------------------------------------------------

    @Test
    fun `恢复草稿后继续编辑沿用原草稿id`() {
        draftStore.drafts["origin"] = newDraft("origin", null, "原草稿", "原内容", updatedTime = 100L)
        val vm = createViewModel()
        scheduler.runCurrent()

        vm.continuePendingDraft()
        vm.updateContent("原内容\n追加一行")
        advance(EditorViewModel.DRAFT_AUTOSAVE_DELAY_MS)

        assertEquals(1, draftStore.drafts.size)
        val updated = draftStore.drafts.values.single()
        assertEquals("origin", updated.draftId)
        assertEquals("原内容\n追加一行", updated.contentMarkdown)

        var finished = false
        vm.finishDiary { finished = true }
        scheduler.runCurrent()
        assertTrue(finished)
        assertEquals("origin", diaryStore.diaries.keys.single())
        assertTrue(draftStore.drafts.isEmpty())
    }

    // ------------------------------------------------------------------
    // 8. 草稿孤儿配图清理
    // ------------------------------------------------------------------

    @Test
    fun `删除新建草稿时孤儿配图文件一并清理`() {
        val tempFile = File.createTempFile("draft_orphan", ".webp").apply { writeText("img") }
        assertTrue(tempFile.exists())

        val attachment = Attachment(
            id = UUID.randomUUID().toString(),
            diaryId = "draft-with-img",
            localPath = tempFile.absolutePath,
            fileName = tempFile.name
        )
        draftStore.drafts["draft-with-img"] = DiaryDraft(
            draftId = "draft-with-img",
            diaryId = null,
            userId = "local_guest",
            title = "带图草稿",
            contentMarkdown = "x",
            entryDate = 1L,
            createdTime = 1L,
            updatedTime = 1L,
            attachments = listOf(attachment)
        )

        val vm = createViewModel()
        scheduler.runCurrent()
        vm.discardPendingDraft()
        scheduler.runCurrent()

        assertTrue("草稿应被删除", draftStore.drafts.isEmpty())
        assertFalse("孤儿图片应被清理", tempFile.exists())
    }
}
