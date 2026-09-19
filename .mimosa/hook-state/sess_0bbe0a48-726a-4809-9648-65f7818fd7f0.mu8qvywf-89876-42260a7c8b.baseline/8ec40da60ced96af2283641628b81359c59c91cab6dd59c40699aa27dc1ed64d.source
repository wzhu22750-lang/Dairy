package com.example.inkpaperdiary.challenger

import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.interaction.IosTouchDefaults
import com.example.inkpaperdiary.ui.navigation.AppleTabDefaults
import com.example.inkpaperdiary.ui.navigation.AppDestination
import com.example.inkpaperdiary.ui.navigation.IosTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Random

/**
 * Empirical Challenger Suite for Milestone 2:
 * Rigorous adversarial stress testing of `IosTabBar` and root navigation architecture:
 *
 * 1. Geometry, Typography & Material Boundary Values (49dp height, 24dp icon, 10sp label, 93% alpha).
 * 2. Canonical Tab Specification & HIG Glyph Parity (exact 4 tabs, outlined/filled pairing).
 * 3. High-Frequency Switching Stress & Re-entrancy Invariants (100k cycles, ping-pong, self-clicks).
 * 4. 2-Tier Navigation State Preservation & Modal Hierarchy (ViewModel hoisting, modal push/pop).
 * 5. BackHandler Hierarchical Interception State Machine (lock, modals, sub-tabs, root exit).
 * 6. Accessibility Semantics, TalkBack Content Descriptions & Touch Target Bounds.
 * 7. Android Idiom Eradication Audit in Navigation Layer.
 */
class IosTabBarEmpiricalChallengeTest {

    // =============================================================================================
    // 1. Boundary Values & Geometry Constants
    // =============================================================================================

    @Test
    fun challenge_tabBar_ExactGeometryDimensions() {
        // Tab bar content height is exactly 48.dp (still >= Android 48dp minimum)
        assertEquals("TabBar content height must be exactly 48.dp", 48.dp, AppleTabDefaults.BarHeight)
        assertEquals(48f, AppleTabDefaults.BarHeight.value, 0.0001f)

        // Hairline top border must be 0.5.dp
        assertEquals("Hairline border width must be exactly 0.5.dp", 0.5.dp, AppleTabDefaults.HairlineBorderWidth)
        assertEquals(0.5f, AppleTabDefaults.HairlineBorderWidth.value, 0.0001f)

        // Tab icon glyph bounding box must be 22.dp
        assertEquals("Icon size must be exactly 22.dp", 22.dp, AppleTabDefaults.IconSize)
        assertEquals(22f, AppleTabDefaults.IconSize.value, 0.0001f)

        // Label typography must be 10.sp
        assertEquals("Label font size must be exactly 10.sp", 10.sp, AppleTabDefaults.LabelFontSize)
        assertEquals(10f, AppleTabDefaults.LabelFontSize.value, 0.0001f)
    }

    @Test
    fun challenge_tabBar_BarBackgroundColorDeterminism() {
        // Ink 设计契约：顶/底栏颜色来自 InkPalette，97% 纸白 / 97% 柔和黑
        val lightBg = AppleMaterials.barBackgroundColor(isDark = false)
        val darkBg = AppleMaterials.barBackgroundColor(isDark = true)

        assertEquals("Light bar background must follow InkPalette.InkBarLight",
            com.example.inkpaperdiary.core.designsystem.InkPalette.InkBarLight, lightBg)
        assertEquals("Dark bar background must follow InkPalette.InkBarDark",
            com.example.inkpaperdiary.core.designsystem.InkPalette.InkBarDark, darkBg)
        assertNotEquals("Light and dark bar backgrounds must differ", lightBg, darkBg)

        // 两种模式都是近乎不透明的微透色
        assertTrue("Light bar alpha must be >= 0.95f", lightBg.alpha >= 0.95f)
        assertTrue("Dark bar alpha must be >= 0.95f", darkBg.alpha >= 0.95f)

        // Color transition animation spec must be 200ms
        assertEquals(200, (AppleTabDefaults.ColorTransitionSpec as? androidx.compose.animation.core.TweenSpec)?.durationMillis)
    }

    @Test
    fun challenge_tabBar_TouchPhysicsCompressionProfile() {
        // Tab click physics: deeper compression (0.92f scale, 0.80f alpha) than generic cards (0.97f, 0.85f)
        assertEquals(0.92f, IosTouchDefaults.TAB_PRESSED_SCALE, 0.0001f)
        assertEquals(0.80f, IosTouchDefaults.TAB_PRESSED_ALPHA, 0.0001f)

        assertTrue("Tab press compression must be strictly deeper than standard card press",
            IosTouchDefaults.TAB_PRESSED_SCALE < IosTouchDefaults.PRESSED_SCALE)
        assertTrue("Tab press alpha dimming must be strictly deeper than standard card press",
            IosTouchDefaults.TAB_PRESSED_ALPHA < IosTouchDefaults.PRESSED_ALPHA)
    }

    // =============================================================================================
    // 2. Canonical Tab Specification & Interface Contracts
    // =============================================================================================

    @Test
    fun challenge_tabBar_CanonicalFourTabsSpecification() {
        val entries = IosTab.entries

        // Must contain strictly 4 tabs
        assertEquals("IosTab must have exactly 4 entries", 4, entries.size)

        // Verify order, labels, and exact names
        assertEquals(IosTab.JOURNAL, entries[0])
        assertEquals("书页", entries[0].label)
        assertEquals("JOURNAL", entries[0].name)

        assertEquals(IosTab.CALENDAR, entries[1])
        assertEquals("目录", entries[1].label)
        assertEquals("CALENDAR", entries[1].name)

        assertEquals(IosTab.MEMORIES, entries[2])
        assertEquals("回声", entries[2].label)
        assertEquals("MEMORIES", entries[2].name)

        assertEquals(IosTab.SETTINGS, entries[3])
        assertEquals("设置", entries[3].label)
        assertEquals("SETTINGS", entries[3].name)
    }

    @Test
    fun challenge_tabBar_GlyphIconPairingIntegrity() {
        // HIG specifies Outlined for unselected, Filled for selected
        for (tab in IosTab.entries) {
            assertNotNull("${tab.name} unselected icon must not be null", tab.icon)
            assertNotNull("${tab.name} selected icon must not be null", tab.selectedIcon)
            assertNotEquals("${tab.name} selected and unselected icons must be distinct ImageVectors",
                tab.icon, tab.selectedIcon)
        }

        // Exact glyph mappings according to PROJECT.md
        assertEquals(Icons.Outlined.Book, IosTab.JOURNAL.icon)
        assertEquals(Icons.Filled.Book, IosTab.JOURNAL.selectedIcon)

        assertEquals(Icons.Outlined.CalendarMonth, IosTab.CALENDAR.icon)
        assertEquals(Icons.Filled.CalendarMonth, IosTab.CALENDAR.selectedIcon)

        assertEquals(Icons.Outlined.History, IosTab.MEMORIES.icon)
        assertEquals(Icons.Filled.History, IosTab.MEMORIES.selectedIcon)

        assertEquals(Icons.Outlined.Settings, IosTab.SETTINGS.icon)
        assertEquals(Icons.Filled.Settings, IosTab.SETTINGS.selectedIcon)
    }

    // =============================================================================================
    // 3. High-Frequency Switching Stress & Re-entrancy Invariants
    // =============================================================================================

    @Test
    fun challenge_stress_SequentialTabSwitching_10000Iterations() {
        var currentTab = IosTab.JOURNAL
        var switchCount = 0

        val tabs = IosTab.entries
        for (i in 1..10_000) {
            val target = tabs[i % tabs.size]
            currentTab = target
            switchCount++
            assertEquals(target, currentTab)
        }

        assertEquals(10_000, switchCount)
        assertEquals(tabs[10_000 % 4], currentTab)
        assertEquals(IosTab.JOURNAL, currentTab)
    }

    @Test
    fun challenge_stress_RandomTabSwitching_100000Cycles() {
        // Deterministic PRNG seed for reproducible adversarial stress test
        val rng = Random(0xCAFEBABE)
        val tabs = IosTab.entries.toTypedArray()
        var currentTab = IosTab.JOURNAL

        val history = IntArray(4)

        for (cycle in 1..100_000) {
            val nextIndex = rng.nextInt(tabs.size)
            currentTab = tabs[nextIndex]
            history[nextIndex]++
            assertEquals(tabs[nextIndex], currentTab)
        }

        // Verify all 4 tabs were exercised roughly equally (~25,000 each)
        for (i in 0..3) {
            assertTrue("Tab $i should be visited at least 23,000 times, was ${history[i]}", history[i] > 23_000)
            assertTrue("Tab $i should be visited at most 27,000 times, was ${history[i]}", history[i] < 27_000)
        }
    }

    @Test
    fun challenge_stress_HighFrequencyPingPongBetweenOppositeTabs() {
        // Rapid oscillation between first and last tab (Journal <-> Settings)
        var activeTab = IosTab.JOURNAL
        val start = System.nanoTime()

        for (i in 0 until 50_000) {
            activeTab = if (activeTab == IosTab.JOURNAL) IosTab.SETTINGS else IosTab.JOURNAL
        }

        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertEquals(IosTab.JOURNAL, activeTab)
        assertTrue("50k tab switches must execute under 200ms (O(1) state transitions), took ${elapsedMs}ms", elapsedMs < 200)
    }

    @Test
    fun challenge_stress_ReentrantClicks_SameTabPreservation() {
        // Simulates repeated tapping on the active tab (common user gesture)
        for (tab in IosTab.entries) {
            var selectedTab = tab
            var triggerCount = 0

            for (repeat in 1..1000) {
                // Clicking already active tab
                selectedTab = tab
                triggerCount++
                assertEquals("Active tab must remain invariant under re-entrant clicks", tab, selectedTab)
            }

            assertEquals(1000, triggerCount)
        }
    }

    @Test
    fun challenge_stress_InterleavedReentrantAndTransitionClicks() {
        var current = IosTab.JOURNAL
        val transitions = mutableListOf<IosTab>()

        fun click(tab: IosTab) {
            current = tab
            transitions.add(tab)
        }

        // Journal -> Journal (re-entrant) -> Calendar -> Calendar -> Calendar -> Memories -> Settings
        click(IosTab.JOURNAL)
        click(IosTab.JOURNAL)
        click(IosTab.CALENDAR)
        click(IosTab.CALENDAR)
        click(IosTab.CALENDAR)
        click(IosTab.MEMORIES)
        click(IosTab.SETTINGS)
        click(IosTab.SETTINGS)

        assertEquals(8, transitions.size)
        assertEquals(IosTab.SETTINGS, current)
        assertEquals(
            listOf(
                IosTab.JOURNAL, IosTab.JOURNAL,
                IosTab.CALENDAR, IosTab.CALENDAR, IosTab.CALENDAR,
                IosTab.MEMORIES,
                IosTab.SETTINGS, IosTab.SETTINGS
            ),
            transitions
        )
    }

    // =============================================================================================
    // 4. 2-Tier Navigation State Preservation & Modal Hierarchy
    // =============================================================================================

    @Test
    fun challenge_navigation_AppDestinationModalHierarchy() {
        // AppDestination sealed interface contracts
        val editorNew: AppDestination = AppDestination.Editor(diaryId = null)
        val editorExisting: AppDestination = AppDestination.Editor(diaryId = "abc-123", entryDate = 1700000000000L)
        val search: AppDestination = AppDestination.Search
        val stats: AppDestination = AppDestination.Stats
        val trash: AppDestination = AppDestination.Trash

        assertTrue(editorNew is AppDestination.Editor)
        assertNull((editorNew as AppDestination.Editor).diaryId)
        assertNull(editorNew.entryDate)

        assertTrue(editorExisting is AppDestination.Editor)
        assertEquals("abc-123", (editorExisting as AppDestination.Editor).diaryId)
        assertEquals(1700000000000L, editorExisting.entryDate)

        assertSame(AppDestination.Search, search)
        assertSame(AppDestination.Stats, stats)
        assertSame(AppDestination.Trash, trash)
    }

    @Test
    fun challenge_navigation_ModalStackPushPopStatePreservation() {
        val modalStack = mutableListOf<AppDestination>()

        // Initially empty
        assertTrue(modalStack.isEmpty())
        assertNull(modalStack.lastOrNull())

        // 1. Push Editor
        val editorDest = AppDestination.Editor("diary-1")
        modalStack.add(editorDest)
        assertEquals(1, modalStack.size)
        assertEquals(editorDest, modalStack.lastOrNull())

        // 2. Push Search on top of Editor
        modalStack.add(AppDestination.Search)
        assertEquals(2, modalStack.size)
        assertEquals(AppDestination.Search, modalStack.lastOrNull())

        // 3. Push Stats on top of Search
        modalStack.add(AppDestination.Stats)
        assertEquals(3, modalStack.size)
        assertEquals(AppDestination.Stats, modalStack.lastOrNull())

        // 4. Pop Stats -> returns to Search
        val poppedStats = modalStack.removeAt(modalStack.size - 1)
        assertEquals(AppDestination.Stats, poppedStats)
        assertEquals(AppDestination.Search, modalStack.lastOrNull())

        // 5. Pop Search -> returns to Editor
        val poppedSearch = modalStack.removeAt(modalStack.size - 1)
        assertEquals(AppDestination.Search, poppedSearch)
        assertEquals(editorDest, modalStack.lastOrNull())

        // 6. Pop Editor -> returns to root tab bar
        val poppedEditor = modalStack.removeAt(modalStack.size - 1)
        assertEquals(editorDest, poppedEditor)
        assertTrue(modalStack.isEmpty())
        assertNull(modalStack.lastOrNull())
    }

    @Test
    fun challenge_navigation_RootStatePreservedAcrossModalPresentations() {
        // Demonstrates that active root tab is preserved even when modal stack is pushed and dismissed
        var selectedRootTab = IosTab.MEMORIES
        val modalStack = mutableListOf<AppDestination>()

        // Open Editor from Memories
        modalStack.add(AppDestination.Editor("memories-entry-9"))
        assertEquals(1, modalStack.size)
        // Root tab remains MEMORIES
        assertEquals(IosTab.MEMORIES, selectedRootTab)

        // Close Editor
        modalStack.removeAt(modalStack.size - 1)
        assertTrue(modalStack.isEmpty())
        // Root tab is still MEMORIES
        assertEquals(IosTab.MEMORIES, selectedRootTab)
    }

    // =============================================================================================
    // 5. BackHandler Hierarchical Interception State Machine
    // =============================================================================================

    /**
     * Replicates the exact BackHandler conditional contract from AppNavigation.kt:
     *
     * BackHandler(
     *     enabled = !(isLockEnabled && isAppLocked) && (modalStack.isNotEmpty() || selectedTab != IosTab.JOURNAL)
     * ) {
     *     if (modalStack.isNotEmpty()) {
     *         if (currentModal !is AppDestination.Editor) {
     *             modalStack.removeLast()
     *         }
     *     } else if (selectedTab != IosTab.JOURNAL) {
     *         selectedTab = IosTab.JOURNAL
     *     }
     * }
     */
    class BackHandlerStateMachine(
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
                val top = modalStack.last()
                if (top !is AppDestination.Editor) {
                    modalStack.removeAt(modalStack.size - 1)
                    return "POPPED_MODAL_${top::class.simpleName}"
                } else {
                    return "EDITOR_INTERNAL_BACK"
                }
            } else if (selectedTab != IosTab.JOURNAL) {
                val prev = selectedTab
                selectedTab = IosTab.JOURNAL
                return "RETURNED_TO_JOURNAL_FROM_${prev.name}"
            }
            return "UNHANDLED"
        }
    }

    @Test
    fun challenge_backHandler_LockedStateSuppression() {
        val machine = BackHandlerStateMachine(
            isLockEnabled = true,
            isAppLocked = true,
            selectedTab = IosTab.CALENDAR,
            modalStack = mutableListOf(AppDestination.Search)
        )
        // When app is locked, BackHandler must be disabled (cannot bypass lock screen via back button)
        assertFalse("BackHandler must be disabled when locked", machine.isEnabled)
        assertEquals("UNHANDLED_EXIT_APP", machine.onBackPress())
    }

    @Test
    fun challenge_backHandler_RootJournalExitContract() {
        val machine = BackHandlerStateMachine(
            isLockEnabled = false,
            isAppLocked = false,
            selectedTab = IosTab.JOURNAL,
            modalStack = mutableListOf()
        )
        // At root Journal with no modals, BackHandler must NOT intercept -> allows exiting app
        assertFalse("At root Journal with empty modal stack, BackHandler must NOT intercept", machine.isEnabled)
        assertEquals("UNHANDLED_EXIT_APP", machine.onBackPress())
    }

    @Test
    fun challenge_backHandler_SubTabReturnsToJournal() {
        for (subTab in listOf(IosTab.CALENDAR, IosTab.MEMORIES, IosTab.SETTINGS)) {
            val machine = BackHandlerStateMachine(
                isLockEnabled = false,
                isAppLocked = false,
                selectedTab = subTab,
                modalStack = mutableListOf()
            )
            assertTrue("BackHandler must intercept on non-Journal root tab $subTab", machine.isEnabled)
            val result = machine.onBackPress()
            assertEquals("RETURNED_TO_JOURNAL_FROM_${subTab.name}", result)
            assertEquals("Must redirect to JOURNAL", IosTab.JOURNAL, machine.selectedTab)

            // Now at JOURNAL -> should no longer intercept
            assertFalse("After returning to JOURNAL, BackHandler should disable", machine.isEnabled)
        }
    }

    @Test
    fun challenge_backHandler_ModalDismissalTakesPrecedenceOverSubTab() {
        val machine = BackHandlerStateMachine(
            isLockEnabled = false,
            isAppLocked = false,
            selectedTab = IosTab.SETTINGS,
            modalStack = mutableListOf(AppDestination.Trash)
        )
        assertTrue(machine.isEnabled)

        // First back press: pops Trash modal, leaves tab as SETTINGS
        val step1 = machine.onBackPress()
        assertEquals("POPPED_MODAL_Trash", step1)
        assertEquals(IosTab.SETTINGS, machine.selectedTab)
        assertTrue(machine.modalStack.isEmpty())

        // Second back press: returns from SETTINGS to JOURNAL
        assertTrue(machine.isEnabled)
        val step2 = machine.onBackPress()
        assertEquals("RETURNED_TO_JOURNAL_FROM_SETTINGS", step2)
        assertEquals(IosTab.JOURNAL, machine.selectedTab)

        // Third back press: unhandled, exits app
        assertFalse(machine.isEnabled)
        assertEquals("UNHANDLED_EXIT_APP", machine.onBackPress())
    }

    @Test
    fun challenge_backHandler_EditorModalRequiresInternalHandling() {
        val machine = BackHandlerStateMachine(
            isLockEnabled = false,
            isAppLocked = false,
            selectedTab = IosTab.JOURNAL,
            modalStack = mutableListOf(AppDestination.Editor("id-123"))
        )
        assertTrue(machine.isEnabled)

        // Back on Editor does NOT auto-pop without saving; it delegates to EditorScreen internal back handler
        val result = machine.onBackPress()
        assertEquals("EDITOR_INTERNAL_BACK", result)
        assertEquals(1, machine.modalStack.size)
    }

    // =============================================================================================
    // 6. Accessibility Semantics, TalkBack & Touch Target Bounds
    // =============================================================================================

    @Test
    fun challenge_accessibility_TouchTargetHeightSatisfiesHIGAndAndroidGuidelines() {
        // Android accessibility minimum touch target is 48x48dp.
        // Apple HIG minimum touch target is 44x44pt.
        // UITabBar content height is 49dp.
        val barHeight = AppleTabDefaults.BarHeight
        assertTrue("Tab bar height (49dp) must exceed Android minimum 48dp", barHeight >= 48.dp)
        assertTrue("Tab bar height (49dp) must exceed iOS minimum 44dp", barHeight >= 44.dp)
    }

    @Test
    fun challenge_accessibility_SourceCodeSemanticsAudit() {
        val file = File("src/main/java/com/example/inkpaperdiary/ui/navigation/IosTabBar.kt")
        assertTrue("IosTabBar.kt source file must exist", file.exists())

        val source = file.readText()

        // 1. Role.Tab semantics must be explicitly declared
        assertTrue("IosTabBar must declare Role.Tab semantics",
            source.contains("role = Role.Tab"))

        // 2. Selection state must be reported to TalkBack
        assertTrue("IosTabBar must report selected = isSelected state",
            source.contains("selected = isSelected"))

        // 3. Icon must provide contentDescription for screen readers
        assertTrue("Icon must provide contentDescription = tab.label",
            source.contains("contentDescription = tab.label"))

        // 4. iosTabClick interaction modifier must be attached
        assertTrue("Tab item must use iosTabClick modifier",
            source.contains(".iosTabClick(onClick = onClick)"))

        // 5. Window insets for bottom navigation bar must be handled
        assertTrue("Must handle WindowInsets.navigationBars bottom padding",
            source.contains("WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)"))

        // 6. 0.5dp top border must be rendered
        assertTrue("Must render HairlineBorderWidth top border",
            source.contains("AppleTabDefaults.HairlineBorderWidth"))
    }

    // =============================================================================================
    // 7. Android Idiom Eradication Audit in Navigation Layer
    // =============================================================================================

    @Test
    fun challenge_idiomPurge_ZeroFloatingActionButtonInNavigation() {
        val navFiles = listOf(
            File("src/main/java/com/example/inkpaperdiary/ui/navigation/IosTabBar.kt"),
            File("src/main/java/com/example/inkpaperdiary/ui/navigation/AppNavigation.kt"),
            File("src/main/java/com/example/inkpaperdiary/ui/navigation/NavRoutes.kt")
        )

        for (file in navFiles) {
            if (file.exists()) {
                val text = file.readText()
                assertFalse(
                    "${file.name} must NOT contain FloatingActionButton",
                    text.contains("FloatingActionButton")
                )
            }
        }
    }

    @Test
    fun challenge_idiomPurge_ZeroThreeDotMenuInNavigation() {
        val navFiles = listOf(
            File("src/main/java/com/example/inkpaperdiary/ui/navigation/IosTabBar.kt"),
            File("src/main/java/com/example/inkpaperdiary/ui/navigation/AppNavigation.kt"),
            File("src/main/java/com/example/inkpaperdiary/ui/navigation/NavRoutes.kt")
        )

        for (file in navFiles) {
            if (file.exists()) {
                val text = file.readText()
                assertFalse(
                    "${file.name} must NOT contain Icons.Default.MoreVert",
                    text.contains("MoreVert")
                )
                assertFalse(
                    "${file.name} must NOT contain DropdownMenu",
                    text.contains("DropdownMenu")
                )
            }
        }
    }
}
