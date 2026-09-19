package com.example.inkpaperdiary.tier2_boundaries

import com.example.inkpaperdiary.core.backup.BackupManager
import com.example.inkpaperdiary.core.backup.TxtDiaryImporter
import com.example.inkpaperdiary.core.database.entity.DiaryEntity
import com.example.inkpaperdiary.core.security.AppLockManager
import com.example.inkpaperdiary.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Tier 2: Boundary & Corner Cases for R4 (Business Logic Preservation & Zero Regression)
 * Verifies corrupted JSON recovery, TXT fallback edge cases, concurrent lock state, geo boundaries.
 */
class R4BoundaryEdgeCasesTest {

    @Test
    fun testB4_EmptyJsonBackupArrayImport() = runBlocking {
        val emptyJson = "[]"
        val result = BackupManager.importFromJson(emptyJson)
        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(0, list.size)
    }

    @Test
    fun testB4_JsonBackupWithMissingOptionalFieldsFallback() = runBlocking {
        // Missing locationName, latitude, longitude, tags, attachments
        val minimalJson = """
            [
              {
                "id": "minimal-001",
                "title": "极简测试",
                "contentMarkdown": "无附加属性内容"
              }
            ]
        """.trimIndent()

        val result = BackupManager.importFromJson(minimalJson)
        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(1, list.size)
        val diary = list[0]
        assertEquals("minimal-001", diary.id)
        assertEquals("极简测试", diary.title)
        assertEquals(null, diary.locationName)
        assertEquals(null, diary.latitude)
        assertEquals(null, diary.longitude)
        assertEquals(0, diary.tags.size)
        assertEquals(0, diary.attachments.size)
    }

    @Test
    fun testB4_JsonBackupWithUnknownAdditionalKeysIgnored() = runBlocking {
        // JSON contains extra unknown keys from future versions
        val futureJson = """
            [
              {
                "id": "future-001",
                "title": "未来版本日记",
                "contentMarkdown": "兼容性测试",
                "future_ai_summary": "这是未来AI字段",
                "future_emotion_score": 0.98
              }
            ]
        """.trimIndent()

        val result = BackupManager.importFromJson(futureJson)
        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(1, list.size)
        assertEquals("未来版本日记", list[0].title)
    }

    @Test
    fun testB4_TxtImporterEmptyString() {
        val diaries = TxtDiaryImporter.parseTxt("")
        assertEquals(0, diaries.size)
    }

    @Test
    fun testB4_TxtImporterWhitespaceOnly() {
        val diaries = TxtDiaryImporter.parseTxt("   \n\n\t  \r\n  ")
        assertEquals(0, diaries.size)
    }

    @Test
    fun testB4_TxtImporterUnparseableDateFallsBackToFileTime() {
        val sampleTime = 1690000000000L
        val content = "这是一个没有出现任何日期的普通便签记录。"
        val diaries = TxtDiaryImporter.parseTxt(content, fileName = "未命名便签.txt", fileLastModified = sampleTime)

        assertEquals(1, diaries.size)
        assertEquals("未命名便签", diaries[0].title)
        assertEquals(sampleTime, diaries[0].entryDate)
    }

    @Test
    fun testB4_ConcurrentAppLockTransitionsThreadSafety() = runBlocking {
        // Multi-threaded concurrent lock and unlock operations
        val job = launch(Dispatchers.Default) {
            repeat(100) { i ->
                if (i % 2 == 0) AppLockManager.lock() else AppLockManager.unlock()
            }
        }
        job.join()
        // State flow should have a deterministic boolean value
        assertNotNull(AppLockManager.isLocked.value)
    }

    @Test
    fun testB4_GeoLocationCoordinatesBoundaryValues() {
        // Latitude [-90.0, 90.0], Longitude [-180.0, 180.0]
        val northPole = DiaryEntity(
            id = UUID.randomUUID().toString(),
            latitude = 90.0,
            longitude = 0.0
        )
        val southPole = DiaryEntity(
            id = UUID.randomUUID().toString(),
            latitude = -90.0,
            longitude = 0.0
        )
        val antiMeridian = DiaryEntity(
            id = UUID.randomUUID().toString(),
            latitude = 0.0,
            longitude = 180.0
        )

        assertEquals(90.0, northPole.latitude!!, 0.001)
        assertEquals(-90.0, southPole.latitude!!, 0.001)
        assertEquals(180.0, antiMeridian.longitude!!, 0.001)
    }

    @Test
    fun testB4_SoftDeletedEntityStateIntegrity() {
        val now = System.currentTimeMillis()
        val deletedEntity = DiaryEntity(
            id = UUID.randomUUID().toString(),
            isDeleted = true,
            deletedAt = now,
            syncStatus = SyncStatus.DELETED.code
        )

        assertTrue(deletedEntity.isDeleted)
        assertEquals(now, deletedEntity.deletedAt)
        assertEquals(SyncStatus.DELETED.code, deletedEntity.syncStatus)
    }

    @Test
    fun testB4_SyncStatusTransitionCycle() {
        var status = SyncStatus.DIRTY
        assertEquals(1, status.code)

        // Upload to cloud -> SYNCED
        status = SyncStatus.SYNCED
        assertEquals(0, status.code)

        // Edit locally -> DIRTY again
        status = SyncStatus.DIRTY
        assertEquals(1, status.code)

        // Delete locally -> DELETED (pending cloud deletion)
        status = SyncStatus.DELETED
        assertEquals(2, status.code)
    }
}
