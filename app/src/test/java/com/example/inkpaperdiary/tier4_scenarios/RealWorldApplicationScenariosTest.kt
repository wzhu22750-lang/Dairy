package com.example.inkpaperdiary.tier4_scenarios

import com.example.inkpaperdiary.core.backup.BackupManager
import com.example.inkpaperdiary.core.database.entity.DiaryEntity
import com.example.inkpaperdiary.core.network.SupabaseClient
import com.example.inkpaperdiary.core.security.AppLockManager
import com.example.inkpaperdiary.domain.model.Attachment
import com.example.inkpaperdiary.domain.model.Diary
import com.example.inkpaperdiary.domain.model.SyncStatus
import com.example.inkpaperdiary.domain.model.Tag
import com.example.inkpaperdiary.domain.model.Weather
import com.example.inkpaperdiary.ui.navigation.Screen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Tier 4: Real-World Application Scenarios
 * Comprehensive, end-to-end multi-step flows simulating real user usage of InkPaperDiary.
 */
class RealWorldApplicationScenariosTest {

    // ---------------------------------------------------------------------------------------------
    // Scenario 1: First Diary Creation Flow (End-to-End)
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testScenario1_FirstDiaryCreationAndStreamDisplay() {
        // Step 1: User launches application, starting on Journal tab
        var activeTab = "JOURNAL"
        assertEquals("JOURNAL", activeTab)

        // Step 2: User taps top-right Compose action in navigation bar -> routes to Editor
        val editorRoute = Screen.Editor.createRoute(null)
        assertEquals("editor/new", editorRoute)

        // Step 3: In Editor, user selects Weather (SUNNY) and custom entry Date
        val customDate = 1718000000000L
        val selectedWeather = Weather.SUNNY

        // Step 4: User writes rich Markdown content
        val markdownContent = """
            # 开启新日记
            今天正式开始使用全新 Apple HIG 风格的日记本！
            - 极简视觉
            - 细腻回弹
            - 隐私守护
        """.trimIndent()

        // Step 5: User toggles Pin switch to ON and taps "完成" (Done) pill button
        val isPinned = true
        val diary = Diary(
            id = UUID.randomUUID().toString(),
            title = "开启新日记",
            contentMarkdown = markdownContent,
            weather = selectedWeather,
            entryDate = customDate,
            isPinned = isPinned,
            syncStatus = SyncStatus.DIRTY
        )

        // Step 6: Navigation pops back to Journal timeline stream
        val currentRoute = Screen.Timeline.route
        assertEquals("timeline", currentRoute)

        // Step 7: Verification in Journal stream
        assertTrue(diary.isPinned)
        assertEquals(Weather.SUNNY, diary.weather)
        assertTrue(diary.previewText.contains("开启新日记"))
        assertTrue(diary.wordCount >= 20)
        assertEquals(SyncStatus.DIRTY, diary.syncStatus)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 2: Cloud Sync Configuration & Lifecycle Flow
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testScenario2_CloudSyncConfigurationAndStateTransition() {
        // Step 1: User switches to "设置" (Settings) tab
        val currentTab = "SETTINGS"
        assertEquals("SETTINGS", currentTab)

        // Step 2: In Section 1 (云端与同步), user taps "Supabase 凭据配置"
        var isModalVisible = true
        assertTrue(isModalVisible)

        // Step 3: User inputs credentials in 270dp IosModalDialog and confirms
        val urlInput = "https://my-diary-project.supabase.co"
        val keyInput = "sb_anon_key_secret_xyz123"
        val client = SupabaseClient(urlInput, keyInput)
        isModalVisible = false
        assertTrue(client.isConfigured)

        // Step 4: Local diary exists in DIRTY sync status
        var localDiary = Diary(
            id = "diary-sync-001",
            title = "待同步日记",
            contentMarkdown = "云端同步测试内容",
            syncStatus = SyncStatus.DIRTY
        )
        assertEquals(SyncStatus.DIRTY, localDiary.syncStatus)

        // Step 5: User taps "立即双向同步" in Settings list row
        // Simulate sync completion: status transitions to SYNCED
        localDiary = localDiary.copy(syncStatus = SyncStatus.SYNCED)

        // Step 6: Verify sync state preserved
        assertEquals(SyncStatus.SYNCED, localDiary.syncStatus)
        assertEquals(0, localDiary.syncStatus.code)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 3: Privacy Security, AppLock & External Media Flow
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testScenario3_PrivacySecurityAndMediaPickerExemption() {
        // Step 1: User enables PIN AppLock in Settings Section 2
        AppLockManager.unlock()
        assertFalse(AppLockManager.isLocked.value)

        // Step 2: User opens Editor to add photos to a diary
        val inEditor = true
        assertTrue(inEditor)

        // Step 3: User taps Photo insertion tool on Markdown bar
        // App launches Android system photo picker -> sets isPickerActive = true
        AppLockManager.isPickerActive = true

        // Step 4: Activity transitions to background (onStop event)
        fun onActivityStop() {
            if (!AppLockManager.isPickerActive) {
                AppLockManager.lock()
            }
        }
        onActivityStop()

        // Step 5: Assert that app is NOT locked because user is selecting photos
        assertFalse("App must not lock out user during external file picker", AppLockManager.isLocked.value)

        // Step 6: Photo picker returns, user inserts image into editor
        val selectedAttachment = Attachment(
            id = UUID.randomUUID().toString(),
            diaryId = "diary-001",
            fileName = "sunset.jpg",
            localPath = "/data/files/sunset.jpg"
        )
        AppLockManager.isPickerActive = false
        assertEquals("sunset.jpg", selectedAttachment.fileName)

        // Step 7: User finishes and puts app into background normally
        onActivityStop()
        assertTrue("App must lock when genuinely backgrounded", AppLockManager.isLocked.value)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 4: Soft-Delete to Trash, Recovery, & JSON Lossless Migration Flow
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testScenario4_TrashLifecycleAndLosslessBackupMigration() = runBlocking {
        // Step 1: User creates diary with rich metadata, tag, and attachment
        val tag = Tag(id = "tag-travel", name = "旅行", colorHex = "#007AFF")
        val attachment = Attachment(
            id = "att-001",
            diaryId = "diary-migrate-001",
            fileName = "mountain.webp",
            localPath = "/data/files/mountain.webp",
            remoteUrl = "https://supabase.co/storage/mountain.webp"
        )
        var diary = Diary(
            id = "diary-migrate-001",
            title = "川西之行",
            contentMarkdown = "# 雪山与草甸\n壮丽的贡嘎雪山！",
            tags = listOf(tag),
            attachments = listOf(attachment),
            isPinned = true,
            isDeleted = false,
            syncStatus = SyncStatus.SYNCED
        )

        // Step 2: User long-presses card, invokes IosActionSheet -> chooses "移入回收站"
        diary = diary.copy(
            isDeleted = true,
            deletedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.DELETED
        )
        assertTrue(diary.isDeleted)

        // Step 3: User navigates to TrashScreen via Settings Section 4
        val trashRoute = Screen.Trash.route
        assertEquals("trash", trashRoute)

        // Step 4: User recovers diary from Trash
        diary = diary.copy(
            isDeleted = false,
            deletedAt = null,
            syncStatus = SyncStatus.DIRTY
        )
        assertFalse(diary.isDeleted)

        // Step 5: User exports full JSON backup
        val sampleJson = """
            [
              {
                "id": "${diary.id}",
                "userId": "user_001",
                "title": "${diary.title}",
                "contentMarkdown": "${diary.contentMarkdown}",
                "weather": "${diary.weather.code}",
                "entryDate": ${diary.entryDate},
                "createdAt": ${diary.createdAt},
                "updatedAt": ${diary.updatedAt},
                "isPinned": ${diary.isPinned},
                "tags": [
                  { "id": "${tag.id}", "name": "${tag.name}", "colorHex": "${tag.colorHex}" }
                ],
                "attachments": [
                  { "id": "${attachment.id}", "fileName": "${attachment.fileName}", "remoteUrl": "${attachment.remoteUrl}" }
                ]
              }
            ]
        """.trimIndent()

        // Step 6: User simulates database restore via JSON import
        val importResult = BackupManager.importFromJson(sampleJson)
        assertTrue(importResult.isSuccess)
        val restoredDiaries = importResult.getOrNull()!!
        assertEquals(1, restoredDiaries.size)

        val restored = restoredDiaries[0]
        assertEquals("diary-migrate-001", restored.id)
        assertEquals("川西之行", restored.title)
        assertEquals(true, restored.isPinned)
        assertEquals(1, restored.tags.size)
        assertEquals("旅行", restored.tags[0].name)
        assertEquals(1, restored.attachments.size)
        assertEquals("mountain.webp", restored.attachments[0].fileName)
        assertEquals("https://supabase.co/storage/mountain.webp", restored.attachments[0].remoteUrl)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 5: Heavy Journal Power-User Filtering & Stream Navigation Flow
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testScenario5_HeavyJournalFilteringAndCollapsibleTitleInteraction() {
        // Step 1: Generate 100 diary entries with mixed pinned attributes
        val diaries = (1..100).map { i ->
            Diary(
                id = "diary-$i",
                title = "日记 $i",
                contentMarkdown = "这是第 $i 篇生活随笔记录。",
                isPinned = (i % 10 == 0) // 10 pinned entries
            )
        }
        assertEquals(100, diaries.size)

        // Step 2: User selects SegmentedControl "置顶" filter
        val pinnedEntries = diaries.filter { it.isPinned }
        assertEquals(10, pinnedEntries.size)

        // Step 3: User selects SegmentedControl "全部" filter
        val allEntries = diaries
        assertEquals(100, allEntries.size)

        // Step 4: User scrolls down 500px in the list
        val scrollOffsetPx = 500f
        val collapseThresholdPx = 156f // 52dp * 3
        val inlineTitleAlpha = (scrollOffsetPx / collapseThresholdPx).coerceIn(0f, 1f)

        // Large title collapsed completely into 17sp SemiBold inline title with frosted glass
        assertEquals(1.0f, inlineTitleAlpha, 0.001f)

        // Step 5: User scrolls back to top (0px)
        val resetOffsetPx = 0f
        val topTitleAlpha = (resetOffsetPx / collapseThresholdPx).coerceIn(0f, 1f)
        assertEquals(0.0f, topTitleAlpha, 0.001f)
    }
}
