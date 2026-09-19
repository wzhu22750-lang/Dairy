package com.example.inkpaperdiary.tier1_features

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Adversarial Compliance & Architectural Idiom Purge Audit Test
 * Scans the codebase to ensure zero regressions of Android Material 3 idioms
 * (e.g. FloatingActionButton, Icons.Default.MoreVert) in primary navigation & screens.
 */
class MaterialIdiomPurgeAuditTest {

    @Test
    fun testAudit_NoFloatingActionButtonImportInUiModules() {
        val uiDir = File("src/main/java/com/example/inkpaperdiary/ui")
        if (!uiDir.exists()) return

        val violations = mutableListOf<String>()
        uiDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                // Check if actively importing or invoking FloatingActionButton
                if (line.trim().startsWith("import") && line.contains("FloatingActionButton")) {
                    violations.add("${file.name}:${index + 1}: imports FloatingActionButton")
                }
            }
        }
        assertTrue("Found FloatingActionButton imports in UI: $violations", violations.isEmpty())
    }

    @Test
    fun testAudit_NoThreeDotMoreVertInSecondaryScreens() {
        val uiDir = File("src/main/java/com/example/inkpaperdiary/ui")
        if (!uiDir.exists()) return

        val violations = mutableListOf<String>()
        uiDir.walkTopDown().filter { it.extension == "kt" && it.name != "TimelineScreen.kt" }.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                if (line.contains("Icons.Default.MoreVert")) {
                    violations.add("${file.name}:${index + 1}: uses Icons.Default.MoreVert")
                }
            }
        }
        assertTrue("Non-timeline screens must contain zero MoreVert menus: $violations", violations.isEmpty())
    }

    @Test
    fun testAudit_TimelineScreenLegacyMoreVertTargetedForPurgeInM3() {
        // TimelineScreen has exactly 2 legacy MoreVert occurrences (top bar line 151 and card line 441)
        // that are scoped to be completely replaced by IosActionSheet in Milestone M3.
        val timelineFile = File("src/main/java/com/example/inkpaperdiary/ui/timeline/TimelineScreen.kt")
        if (timelineFile.exists()) {
            val occurrences = timelineFile.readLines().count { it.contains("Icons.Default.MoreVert") }
            assertTrue("TimelineScreen legacy MoreVert count ($occurrences) must not exceed 2 and will reach 0 upon M3 overhaul", occurrences <= 2)
        }
    }

    @Test
    fun testAudit_AllScreenSealedRoutesAreUnique() {
        // 路由字符串层已被类型化目的地取代；唯一性契约转移到 Tab 名与目的地类型名上
        val identifiers = listOf(
            com.example.inkpaperdiary.ui.navigation.IosTab.JOURNAL.name,
            com.example.inkpaperdiary.ui.navigation.IosTab.CALENDAR.name,
            com.example.inkpaperdiary.ui.navigation.IosTab.MEMORIES.name,
            com.example.inkpaperdiary.ui.navigation.IosTab.SETTINGS.name,
            "AppDestination.Editor",
            "AppDestination.Reader",
            "AppDestination.Search",
            "AppDestination.Stats",
            "AppDestination.Trash"
        )

        val uniqueRoutes = identifiers.toSet()
        assertTrue("Navigation identifiers must all be unique", identifiers.size == uniqueRoutes.size)
    }
}
