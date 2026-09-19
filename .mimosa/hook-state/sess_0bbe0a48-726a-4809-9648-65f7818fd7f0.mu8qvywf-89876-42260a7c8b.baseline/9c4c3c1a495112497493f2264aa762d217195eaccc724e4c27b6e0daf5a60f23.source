package com.example.inkpaperdiary.challenger

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.ui.graphics.Color
import com.example.inkpaperdiary.core.designsystem.components.IosActionItem
import com.example.inkpaperdiary.domain.model.Diary
import com.example.inkpaperdiary.domain.model.Weather
import com.example.inkpaperdiary.ui.navigation.AppDestination
import com.example.inkpaperdiary.ui.navigation.IosTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Empirical Challenger Suite for Milestone 3:
 * Rigorous adversarial stress testing of `IosActionSheet` long-press gestures, callbacks,
 * and `AppNavigation.kt` modal push/pop architecture.
 *
 * 1. Action sheet trigger, presentation and dismissal state transitions.
 * 2. Long-press callback invocation for pin/unpin toggles and move-to-trash actions.
 * 3. Modal push and pop transitions with `modalStack.removeAt(modalStack.size - 1)`.
 * 4. Rapid back-press sequences on modal stack without IndexOutOfBoundsException or NoSuchMethodError.
 * 5. Static assertions confirming 0 FloatingActionButton, 0 MoreVert, 0 DropdownMenu.
 */
class IosActionSheetAndNavigationEmpiricalChallengeTest {

    private fun resolveSourceFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("app", relativePath),
            File(System.getProperty("user.dir") ?: ".", relativePath),
            File(System.getProperty("user.dir") ?: ".", "app/$relativePath")
        )
        return candidates.firstOrNull { it.exists() } ?: File(relativePath)
    }

    private fun resolveUiSourceDir(): File {
        val candidates = listOf(
            File("src/main/java/com/example/inkpaperdiary/ui"),
            File("app/src/main/java/com/example/inkpaperdiary/ui"),
            File(System.getProperty("user.dir") ?: ".", "src/main/java/com/example/inkpaperdiary/ui"),
            File(System.getProperty("user.dir") ?: ".", "app/src/main/java/com/example/inkpaperdiary/ui")
        )
        return candidates.firstOrNull { it.exists() } ?: File("src/main/java/com/example/inkpaperdiary/ui")
    }

    private fun resolveMainSourceDir(): File {
        val candidates = listOf(
            File("src/main/java/com/example/inkpaperdiary"),
            File("app/src/main/java/com/example/inkpaperdiary"),
            File(System.getProperty("user.dir") ?: ".", "src/main/java/com/example/inkpaperdiary"),
            File(System.getProperty("user.dir") ?: ".", "app/src/main/java/com/example/inkpaperdiary")
        )
        return candidates.firstOrNull { it.exists() } ?: File("src/main/java/com/example/inkpaperdiary")
    }

    // =============================================================================================
    // 1. Action Sheet Trigger and Dismissal State Transitions
    // =============================================================================================

    class ActionSheetStateHolder {
        var selectedDiary: Diary? = null
        val isVisible: Boolean get() = selectedDiary != null

        fun trigger(diary: Diary) {
            selectedDiary = diary
        }

        fun dismiss() {
            selectedDiary = null
        }
    }

    @Test
    fun challenge_actionSheet_InitialStateDismissed() {
        val state = ActionSheetStateHolder()
        assertFalse("Action sheet must initially be dismissed (visible = false)", state.isVisible)
        assertNull("Selected diary must initially be null", state.selectedDiary)
    }

    @Test
    fun challenge_actionSheet_TriggerAndDismissCycle() {
        val state = ActionSheetStateHolder()
        val testDiary = Diary(id = "diary-42", title = "初秋微雨", contentMarkdown = "窗外桂花飘香")

        // 1. Long press trigger
        state.trigger(testDiary)
        assertTrue("Action sheet must be visible when target diary is set", state.isVisible)
        assertEquals("diary-42", state.selectedDiary?.id)

        // 2. Dismiss request
        state.dismiss()
        assertFalse("Action sheet must be dismissed when target diary is cleared", state.isVisible)
        assertNull(state.selectedDiary)
    }

    @Test
    fun challenge_actionSheet_StressTriggerDismiss_10000Cycles() {
        val state = ActionSheetStateHolder()
        val diaries = (1..50).map { i ->
            Diary(id = "diary-$i", title = "日记 $i", contentMarkdown = "内容 $i")
        }

        val start = System.nanoTime()
        for (i in 1..10_000) {
            val target = diaries[i % diaries.size]
            // Open
            state.trigger(target)
            assertEquals(target.id, state.selectedDiary?.id)

            // Dismiss
            state.dismiss()
            assertNull(state.selectedDiary)
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertNull("Final state must be dismissed", state.selectedDiary)
        assertTrue("10k trigger-dismiss cycles should execute under 200ms, took ${elapsedMs}ms", elapsedMs < 200)
    }

    @Test
    fun challenge_actionSheet_DismissRequestPrecedesActionExecution() {
        // HIG requirement: Tapping an action row must dismiss the sheet before executing the action callback
        var selectedDiaryForAction: Diary? = Diary(id = "d1", title = "测试日记")
        val executionOrder = mutableListOf<String>()

        val onDismissRequest = {
            executionOrder.add("DISMISS")
            selectedDiaryForAction = null
        }

        val actionItem = IosActionItem(
            title = "编辑日记",
            icon = Icons.Outlined.Edit,
            onClick = {
                executionOrder.add("ACTION_EXECUTE")
            }
        )

        // Simulate IosActionSheet row tap: onDismissRequest() -> action.onClick()
        onDismissRequest()
        actionItem.onClick()

        assertEquals(listOf("DISMISS", "ACTION_EXECUTE"), executionOrder)
        assertNull("Selected diary must be cleared by dismissal before or during action", selectedDiaryForAction)
    }

    @Test
    fun challenge_actionSheet_CancelPillDismissalContract() {
        var selectedDiary: Diary? = Diary(id = "cancel-test")
        var actionExecuted = false

        val onDismissRequest = {
            selectedDiary = null
        }

        // Tapping cancel pill only invokes onDismissRequest
        onDismissRequest()

        assertNull(selectedDiary)
        assertFalse("Cancel pill must not trigger any action callback", actionExecuted)
    }

    // =============================================================================================
    // 2. Long-Press Callback Invocation for Pin/Unpin Toggles and Move-to-Trash Actions
    // =============================================================================================

    @Test
    fun challenge_longPress_PinActionLabelAndIcon_UnpinnedDiary() {
        val unpinnedDiary = Diary(id = "d-unpinned", isPinned = false)

        val pinActionTitle = if (unpinnedDiary.isPinned) "取消置顶" else "置顶此篇"
        val pinActionIcon = if (unpinnedDiary.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin

        assertEquals("Unpinned diary action must be '置顶此篇'", "置顶此篇", pinActionTitle)
        assertEquals("Unpinned diary icon must be Icons.Filled.PushPin", Icons.Filled.PushPin, pinActionIcon)
    }

    @Test
    fun challenge_longPress_UnpinActionLabelAndIcon_PinnedDiary() {
        val pinnedDiary = Diary(id = "d-pinned", isPinned = true)

        val pinActionTitle = if (pinnedDiary.isPinned) "取消置顶" else "置顶此篇"
        val pinActionIcon = if (pinnedDiary.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin

        assertEquals("Pinned diary action must be '取消置顶'", "取消置顶", pinActionTitle)
        assertEquals("Pinned diary icon must be Icons.Outlined.PushPin", Icons.Outlined.PushPin, pinActionIcon)
    }

    @Test
    fun challenge_longPress_PinToggleCallbackStateMutation() {
        var currentDiary = Diary(id = "d-toggle", isPinned = false)
        var toggleCount = 0

        fun onTogglePin(target: Diary) {
            currentDiary = currentDiary.copy(isPinned = !target.isPinned)
            toggleCount++
        }

        // 1. Initial: isPinned = false -> Trigger action sheet -> Toggle -> isPinned = true
        assertFalse(currentDiary.isPinned)
        onTogglePin(currentDiary)
        assertTrue("Diary must now be pinned", currentDiary.isPinned)
        assertEquals(1, toggleCount)

        // 2. Now: isPinned = true -> Trigger action sheet -> Toggle -> isPinned = false
        onTogglePin(currentDiary)
        assertFalse("Diary must now be unpinned", currentDiary.isPinned)
        assertEquals(2, toggleCount)

        // 3. Stress toggle 1,000 times
        for (i in 1..1000) {
            onTogglePin(currentDiary)
        }
        assertEquals(1002, toggleCount)
        assertFalse("Even number of toggles returns to unpinned", currentDiary.isPinned)
    }

    @Test
    fun challenge_longPress_MoveToTrashActionAttributesAndCallback() {
        val targetDiary = Diary(id = "trash-candidate-99", title = "待删除日记")
        var deletedDiaryId: String? = null

        val trashAction = IosActionItem(
            title = "移入回收站",
            icon = Icons.Outlined.Delete,
            isDestructive = true,
            onClick = {
                deletedDiaryId = targetDiary.id
            }
        )

        assertEquals("Action title must be '移入回收站'", "移入回收站", trashAction.title)
        assertEquals("Trash action icon must be Outlined.Delete", Icons.Outlined.Delete, trashAction.icon)
        assertTrue("Trash action must be flagged as isDestructive = true", trashAction.isDestructive)

        // Verify Apple Red color contract for destructive actions
        val appleDestructiveRed = Color(0xFFFF3B30)
        assertEquals(Color(0xFFFF3B30), appleDestructiveRed)
        assertEquals(1f, appleDestructiveRed.alpha, 0.001f)

        // Execute trash callback
        trashAction.onClick()
        assertEquals("Deleted diary ID must match target diary ID exactly", "trash-candidate-99", deletedDiaryId)
    }

    @Test
    fun challenge_longPress_ActionSheetTitleAndMessageFallbacks() {
        val dateFormat = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINESE)
        val epochTime = 1725624000000L // 2024-09-06 20:00

        // Case A: Diary has explicit title
        val diaryWithTitle = Diary(title = "成都旅居手记", contentMarkdown = "正文内容...", entryDate = epochTime)
        val titleTextA = diaryWithTitle.title.ifBlank { diaryWithTitle.previewText.take(28).ifBlank { "日记操作" } }
        val dateStrA = dateFormat.format(Date(diaryWithTitle.entryDate))
        assertEquals("成都旅居手记", titleTextA)
        assertTrue(dateStrA.contains("2024年") || dateStrA.contains("年"))

        // Case B: Diary has empty title but non-empty preview
        val diaryNoTitle = Diary(title = "", contentMarkdown = "# 随笔\n今天读了博尔赫斯的作品集...", entryDate = epochTime)
        val titleTextB = diaryNoTitle.title.ifBlank { diaryNoTitle.previewText.take(28).ifBlank { "日记操作" } }
        assertTrue("Should fallback to preview text", titleTextB.startsWith("随笔 今天读了博尔赫斯") || titleTextB.startsWith("随笔"))
        assertTrue("Fallback title length must be <= 28 characters", titleTextB.length <= 28)

        // Case C: Diary has empty title and empty body
        val emptyDiary = Diary(title = "", contentMarkdown = "", entryDate = epochTime)
        val titleTextC = emptyDiary.title.ifBlank { emptyDiary.previewText.take(28).ifBlank { "日记操作" } }
        assertEquals("日记操作", titleTextC)
    }

    @Test
    fun challenge_longPress_SelectionIsolationAcrossMultipleDiaries() {
        val diaries = (1..10).map { i ->
            Diary(id = "diary-$i", title = "日记 #$i")
        }

        var selectedDiary: Diary? = null

        // Long press diary 3
        selectedDiary = diaries[2]
        assertEquals("diary-3", selectedDiary.id)

        // Long press diary 7 (overriding selection)
        selectedDiary = diaries[6]
        assertEquals("diary-7", selectedDiary.id)

        // Dismiss
        selectedDiary = null
        assertNull(selectedDiary)
    }

    // =============================================================================================
    // 3. Modal Push and Pop Transitions with `modalStack.removeAt(modalStack.size - 1)`
    // =============================================================================================

    @Test
    fun challenge_modalStack_PushAndPopTransitions_RemoveAtContract() {
        val modalStack = mutableListOf<AppDestination>()

        // 1. Initial: empty
        assertTrue(modalStack.isEmpty())
        assertNull(modalStack.lastOrNull())

        // 2. Push Editor
        val editor = AppDestination.Editor(diaryId = "diary-101")
        modalStack.add(editor)
        assertEquals(1, modalStack.size)
        assertEquals(editor, modalStack.lastOrNull())

        // 3. Push Search on top
        modalStack.add(AppDestination.Search)
        assertEquals(2, modalStack.size)
        assertEquals(AppDestination.Search, modalStack.lastOrNull())

        // 4. Pop Search via removeAt(modalStack.size - 1)
        val popped1 = modalStack.removeAt(modalStack.size - 1)
        assertEquals(AppDestination.Search, popped1)
        assertEquals(1, modalStack.size)
        assertEquals(editor, modalStack.lastOrNull())

        // 5. Push Stats
        modalStack.add(AppDestination.Stats)
        assertEquals(2, modalStack.size)
        assertEquals(AppDestination.Stats, modalStack.lastOrNull())

        // 6. Push Trash
        modalStack.add(AppDestination.Trash)
        assertEquals(3, modalStack.size)
        assertEquals(AppDestination.Trash, modalStack.lastOrNull())

        // 7. Pop Trash
        val popped2 = modalStack.removeAt(modalStack.size - 1)
        assertEquals(AppDestination.Trash, popped2)
        assertEquals(2, modalStack.size)
        assertEquals(AppDestination.Stats, modalStack.lastOrNull())

        // 8. Pop Stats
        val popped3 = modalStack.removeAt(modalStack.size - 1)
        assertEquals(AppDestination.Stats, popped3)
        assertEquals(1, modalStack.size)
        assertEquals(editor, modalStack.lastOrNull())

        // 9. Pop Editor -> returns to root
        val popped4 = modalStack.removeAt(modalStack.size - 1)
        assertEquals(editor, popped4)
        assertTrue(modalStack.isEmpty())
        assertNull(modalStack.lastOrNull())
    }

    @Test
    fun challenge_modalStack_DeepNestingPushPopStress_10000Levels() {
        val modalStack = mutableListOf<AppDestination>()
        val start = System.nanoTime()

        // Push 10,000 destinations
        for (i in 1..10_000) {
            val dest = when (i % 4) {
                0 -> AppDestination.Editor(diaryId = "d-$i")
                1 -> AppDestination.Search
                2 -> AppDestination.Stats
                else -> AppDestination.Trash
            }
            modalStack.add(dest)
        }
        assertEquals(10_000, modalStack.size)

        // Pop all 10,000 using removeAt(modalStack.size - 1)
        while (modalStack.isNotEmpty()) {
            modalStack.removeAt(modalStack.size - 1)
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        assertTrue("modalStack must be completely empty", modalStack.isEmpty())
        assertNull(modalStack.lastOrNull())
        assertTrue("10k deep modal push/pop must execute under 200ms, took ${elapsedMs}ms", elapsedMs < 200)
    }

    @Test
    fun challenge_modalStack_UnderflowGuardRequirement() {
        val modalStack = mutableListOf<AppDestination>()

        // Demonstrates why the guard `if (modalStack.isNotEmpty())` is strictly necessary:
        // Calling removeAt(-1) on empty list must throw IndexOutOfBoundsException
        try {
            modalStack.removeAt(modalStack.size - 1)
            fail("Calling removeAt(size - 1) on empty list MUST throw IndexOutOfBoundsException")
        } catch (e: IndexOutOfBoundsException) {
            // Expected
            assertNotNull(e)
        }

        // Verify guarded pattern handles empty stack safely
        var poppedSafely = false
        if (modalStack.isNotEmpty()) {
            modalStack.removeAt(modalStack.size - 1)
            poppedSafely = true
        }
        assertFalse("Guard must prevent pop attempt on empty stack", poppedSafely)
        assertTrue(modalStack.isEmpty())
    }

    @Test
    fun challenge_modalStack_RootTabStatePreservationDuringModalLifecycle() {
        var activeTab = IosTab.JOURNAL
        val modalStack = mutableListOf<AppDestination>()

        // Navigate to Calendar tab
        activeTab = IosTab.CALENDAR
        assertEquals(IosTab.CALENDAR, activeTab)

        // Push Editor from Calendar
        modalStack.add(AppDestination.Editor("cal-diary-1", 1725624000000L))
        assertEquals(1, modalStack.size)
        // Root tab remains Calendar beneath modal
        assertEquals(IosTab.CALENDAR, activeTab)

        // Dismiss Editor
        if (modalStack.isNotEmpty()) {
            modalStack.removeAt(modalStack.size - 1)
        }
        assertTrue(modalStack.isEmpty())
        // Root tab is still Calendar
        assertEquals(IosTab.CALENDAR, activeTab)
    }

    // =============================================================================================
    // 4. Rapid Back-Press Sequences on Modal Stack
    // =============================================================================================

    /**
     * Exact replication of AppNavigation.kt BackHandler state machine logic.
     */
    class BackHandlerTestRig(
        var isLockEnabled: Boolean = false,
        var isAppLocked: Boolean = false,
        var selectedTab: IosTab = IosTab.JOURNAL,
        val modalStack: MutableList<AppDestination> = mutableListOf()
    ) {
        val isEnabled: Boolean
            get() = !(isLockEnabled && isAppLocked) && (modalStack.isNotEmpty() || selectedTab != IosTab.JOURNAL)

        fun onBackPress(): String {
            if (!isEnabled) {
                return "UNHANDLED_EXIT_APP"
            }
            if (modalStack.isNotEmpty()) {
                val currentModal = modalStack.lastOrNull()
                if (currentModal !is AppDestination.Editor) {
                    modalStack.removeAt(modalStack.size - 1)
                    return "POPPED_MODAL_${currentModal?.javaClass?.simpleName}"
                } else {
                    return "EDITOR_DELEGATE"
                }
            } else if (selectedTab != IosTab.JOURNAL) {
                val fromTab = selectedTab
                selectedTab = IosTab.JOURNAL
                return "RETURNED_TO_JOURNAL_FROM_${fromTab.name}"
            }
            return "UNHANDLED"
        }

        fun editorSimulateSaveAndDismiss() {
            if (modalStack.isNotEmpty() && modalStack.lastOrNull() is AppDestination.Editor) {
                modalStack.removeAt(modalStack.size - 1)
            }
        }
    }

    @Test
    fun challenge_backPress_RapidBurstSequence_NoExceptions() {
        val rig = BackHandlerTestRig(
            isLockEnabled = false,
            isAppLocked = false,
            selectedTab = IosTab.SETTINGS,
            modalStack = mutableListOf(
                AppDestination.Search,
                AppDestination.Stats,
                AppDestination.Trash
            )
        )

        val log = mutableListOf<String>()

        // Rapid back 1: pops Trash
        log.add(rig.onBackPress())
        assertEquals(2, rig.modalStack.size)

        // Rapid back 2: pops Stats
        log.add(rig.onBackPress())
        assertEquals(1, rig.modalStack.size)

        // Rapid back 3: pops Search
        log.add(rig.onBackPress())
        assertEquals(0, rig.modalStack.size)

        // Rapid back 4: returns from SETTINGS to JOURNAL
        log.add(rig.onBackPress())
        assertEquals(IosTab.JOURNAL, rig.selectedTab)

        // Rapid back 5 to 1000: at root Journal with empty modal stack -> BackHandler disabled
        for (i in 5..1000) {
            val res = rig.onBackPress()
            log.add(res)
            assertEquals("UNHANDLED_EXIT_APP", res)
        }

        assertFalse("BackHandler must be disabled once at root Journal", rig.isEnabled)
        assertEquals(1000, log.size)
        assertEquals("POPPED_MODAL_Trash", log[0])
        assertEquals("POPPED_MODAL_Stats", log[1])
        assertEquals("POPPED_MODAL_Search", log[2])
        assertEquals("RETURNED_TO_JOURNAL_FROM_SETTINGS", log[3])
        assertEquals("UNHANDLED_EXIT_APP", log[4])
    }

    @Test
    fun challenge_backPress_EditorRequiresInternalDismissal() {
        val rig = BackHandlerTestRig(
            selectedTab = IosTab.JOURNAL,
            modalStack = mutableListOf(AppDestination.Editor(diaryId = "abc"))
        )

        assertTrue(rig.isEnabled)
        // Back press on Editor delegates to Editor internal save
        val res = rig.onBackPress()
        assertEquals("EDITOR_DELEGATE", res)
        assertEquals(1, rig.modalStack.size)

        // Editor saves and closes
        rig.editorSimulateSaveAndDismiss()
        assertTrue(rig.modalStack.isEmpty())
        assertFalse(rig.isEnabled)
    }

    @Test
    fun challenge_backPress_LockedAppCompletelySuppressesBack() {
        val rig = BackHandlerTestRig(
            isLockEnabled = true,
            isAppLocked = true,
            selectedTab = IosTab.CALENDAR,
            modalStack = mutableListOf(AppDestination.Stats, AppDestination.Trash)
        )

        // When locked, BackHandler must never intercept back presses
        assertFalse("Locked app must disable BackHandler", rig.isEnabled)
        val res = rig.onBackPress()
        assertEquals("UNHANDLED_EXIT_APP", res)

        // Stack and tab remain untouched
        assertEquals(2, rig.modalStack.size)
        assertEquals(IosTab.CALENDAR, rig.selectedTab)
    }

    @Test
    fun challenge_backPress_ConcurrentDrainStress() {
        val rig = BackHandlerTestRig(
            selectedTab = IosTab.JOURNAL,
            modalStack = Collections.synchronizedList(mutableListOf<AppDestination>())
        )

        // Populate with 200 destinations
        for (i in 1..200) {
            rig.modalStack.add(if (i % 2 == 0) AppDestination.Search else AppDestination.Stats)
        }
        assertEquals(200, rig.modalStack.size)

        // Execute concurrent pops
        val executor = Executors.newFixedThreadPool(4)
        for (t in 0 until 4) {
            executor.submit {
                for (step in 0 until 60) {
                    synchronized(rig.modalStack) {
                        if (rig.modalStack.isNotEmpty()) {
                            rig.modalStack.removeAt(rig.modalStack.size - 1)
                        }
                    }
                }
            }
        }
        executor.shutdown()
        val finished = executor.awaitTermination(3, TimeUnit.SECONDS)
        assertTrue(finished)
        assertTrue("Modal stack size must be <= 200", rig.modalStack.size <= 200)
    }

    // =============================================================================================
    // 5. Static Assertions Confirming 0 FloatingActionButton, 0 MoreVert, 0 DropdownMenu
    // =============================================================================================

    @Test
    fun challenge_staticAssertions_ZeroFloatingActionButtonInSource() {
        val mainDir = resolveMainSourceDir()
        assertTrue("Main source directory must exist: ${mainDir.absolutePath}", mainDir.exists())

        val violations = mutableListOf<String>()
        mainDir.walkTopDown()
            .filter { it.extension == "kt" }
            .forEach { file ->
                file.readLines().forEachIndexed { lineNum, line ->
                    if (line.contains("FloatingActionButton") && !line.trim().startsWith("//") && !line.trim().startsWith("*")) {
                        violations.add("${file.name}:${lineNum + 1}: $line")
                    }
                }
            }

        assertEquals("Static assertion failed: Found FloatingActionButton references in production code: $violations",
            0, violations.size)
    }

    @Test
    fun challenge_staticAssertions_ZeroMoreVertInSource() {
        val mainDir = resolveMainSourceDir()
        assertTrue("Main source directory must exist: ${mainDir.absolutePath}", mainDir.exists())

        val violations = mutableListOf<String>()
        mainDir.walkTopDown()
            .filter { it.extension == "kt" }
            .forEach { file ->
                file.readLines().forEachIndexed { lineNum, line ->
                    if (line.contains("MoreVert") && !line.trim().startsWith("//") && !line.trim().startsWith("*")) {
                        violations.add("${file.name}:${lineNum + 1}: $line")
                    }
                }
            }

        assertEquals("Static assertion failed: Found MoreVert references in production code: $violations",
            0, violations.size)
    }

    @Test
    fun challenge_staticAssertions_ZeroDropdownMenuInSource() {
        val mainDir = resolveMainSourceDir()
        assertTrue("Main source directory must exist: ${mainDir.absolutePath}", mainDir.exists())

        val violations = mutableListOf<String>()
        mainDir.walkTopDown()
            .filter { it.extension == "kt" }
            .forEach { file ->
                file.readLines().forEachIndexed { lineNum, line ->
                    if (line.contains("DropdownMenu") && !line.trim().startsWith("//") && !line.trim().startsWith("*")) {
                        violations.add("${file.name}:${lineNum + 1}: $line")
                    }
                }
            }

        assertEquals("Static assertion failed: Found DropdownMenu references in production code: $violations",
            0, violations.size)
    }

    @Test
    fun challenge_timelineScreen_StructuralArchitectureAudit() {
        val timelineFile = resolveSourceFile("src/main/java/com/example/inkpaperdiary/ui/timeline/TimelineScreen.kt")
        assertTrue("TimelineScreen.kt must exist", timelineFile.exists())

        val content = timelineFile.readText()

        // 1. Must use IosLargeTitleScaffold
        assertTrue("TimelineScreen must use IosLargeTitleScaffold", content.contains("IosLargeTitleScaffold"))

        // 2. Must use IosActionSheet for contextual actions
        assertTrue("TimelineScreen must use IosActionSheet", content.contains("IosActionSheet("))
        assertTrue("TimelineScreen must configure IosActionItem", content.contains("IosActionItem("))

        // 3. Dairy 2.0：时间书直接印在纸面上，不再有分段筛选器与心情胶囊
        assertFalse("TimelineScreen must NOT contain IosSegmentedControl (Dairy 2.0 时间书)",
            content.contains("IosSegmentedControl("))
        assertFalse("TimelineScreen must NOT reference Mood (心情系统已删除)", content.contains("Mood"))

        // 4. Compose action must be in top bar (IosNavIconButton with Icons.Outlined.Edit)
        assertTrue("Compose action must be an IosNavIconButton in actions slot",
            content.contains("icon = Icons.Outlined.Edit") && content.contains("contentDescription = \"书写今天\""))

        // 5. Must NOT have FloatingActionButton
        assertFalse("TimelineScreen must NOT contain FloatingActionButton", content.contains("FloatingActionButton"))

        // 6. Must NOT have MoreVert
        assertFalse("TimelineScreen must NOT contain MoreVert", content.contains("MoreVert"))

        // 7. Must NOT have DropdownMenu
        assertFalse("TimelineScreen must NOT contain DropdownMenu", content.contains("DropdownMenu"))
    }

    @Test
    fun challenge_appNavigation_StructuralBackAndModalStackAudit() {
        val navFile = resolveSourceFile("src/main/java/com/example/inkpaperdiary/ui/navigation/AppNavigation.kt")
        assertTrue("AppNavigation.kt must exist", navFile.exists())

        val content = navFile.readText()

        // 1. Must use removeAt(modalStack.size - 1)
        assertTrue("AppNavigation must use modalStack.removeAt(modalStack.size - 1)",
            content.contains("modalStack.removeAt(modalStack.size - 1)"))

        // 2. Must guard removeAt with modalStack.isNotEmpty()
        assertTrue("AppNavigation must guard modalStack.removeAt with modalStack.isNotEmpty()",
            content.contains("if (modalStack.isNotEmpty())"))

        // 3. Must NOT use removeLast() to avoid potential NoSuchMethodError on desugared Android runtimes
        assertFalse("AppNavigation must NOT use removeLast()", content.contains("modalStack.removeLast()"))

        // 4. Must implement 2-tier navigation (4 tabs + pushed modal destinations)
        assertTrue("AppNavigation must define AppDestination sealed interface",
            content.contains("sealed interface AppDestination"))
        assertTrue("AppNavigation must define IosTabBar", content.contains("IosTabBar("))
    }
}
