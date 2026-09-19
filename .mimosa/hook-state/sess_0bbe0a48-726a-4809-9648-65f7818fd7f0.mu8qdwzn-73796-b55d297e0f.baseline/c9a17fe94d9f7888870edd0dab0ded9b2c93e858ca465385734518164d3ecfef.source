package com.example.inkpaperdiary.core.sync

import com.example.inkpaperdiary.core.database.AppDatabase
import com.example.inkpaperdiary.core.database.entity.DiaryEntity
import com.example.inkpaperdiary.core.network.SupabaseClient
import com.example.inkpaperdiary.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SyncManager(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val diaryDao = database.diaryDao()
    private val attachmentDao = database.attachmentDao()
    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun isConfigured(): Boolean {
        val url = settingsRepository.supabaseUrl.first()
        val key = settingsRepository.supabaseAnonKey.first()
        return url.isNotBlank() && key.isNotBlank()
    }

    suspend fun performSync(): Result<Int> = withContext(Dispatchers.IO) {
        val url = settingsRepository.supabaseUrl.first()
        val key = settingsRepository.supabaseAnonKey.first()

        if (url.isBlank() || key.isBlank()) {
            return@withContext Result.failure(Exception("Supabase 未配置，跳过云端同步"))
        }

        val client = SupabaseClient(url, key)
        val testResult = client.testConnection()
        if (testResult.isFailure) {
            return@withContext Result.failure(testResult.exceptionOrNull() ?: Exception("无法连接到 Supabase"))
        }

        var syncedCount = 0

        // 1. 上传本地未同步的媒体图片
        val unsyncedDiaries = diaryDao.getUnsyncedDiaries()
        for (diaryDetails in unsyncedDiaries) {
            for (att in diaryDetails.attachments) {
                if (att.remoteUrl.isNullOrBlank() && File(att.localPath).exists()) {
                    val remotePath = "${diaryDetails.diary.userId}/${att.fileName}"
                    val uploadResult = client.uploadMedia(File(att.localPath), remotePath)
                    if (uploadResult.isSuccess) {
                        val remoteUrl = uploadResult.getOrNull()
                        attachmentDao.insertOrUpdate(att.copy(remoteUrl = remoteUrl, syncStatus = 0))
                    }
                }
            }
        }

        // 2. 将本地修改/删除的日记推送至云端 (Upsert)
        val diariesToPush = unsyncedDiaries.map { d ->
            buildJsonObject {
                put("id", d.diary.id)
                put("user_id", d.diary.userId)
                put("title", d.diary.title)
                put("content_markdown", d.diary.contentMarkdown)
                put("mood", d.diary.mood)
                put("weather", d.diary.weather)
                put("location_name", d.diary.locationName)
                d.diary.latitude?.let { put("latitude", it) }
                d.diary.longitude?.let { put("longitude", it) }
                put("entry_date", isoFormatter.format(Date(d.diary.entryDate)))
                put("created_at", isoFormatter.format(Date(d.diary.createdAt)))
                put("updated_at", isoFormatter.format(Date(d.diary.updatedAt)))
                put("is_pinned", d.diary.isPinned)
                put("is_deleted", d.diary.isDeleted)
                d.diary.deletedAt?.let { put("deleted_at", isoFormatter.format(Date(it))) }
            }
        }

        if (diariesToPush.isNotEmpty()) {
            val pushResult = client.upsertDiaries(JsonArray(diariesToPush))
            if (pushResult.isSuccess) {
                // 将本地状态置为已同步
                for (d in unsyncedDiaries) {
                    diaryDao.updateSyncStatus(d.diary.id, 0)
                }
                syncedCount += diariesToPush.size
            }
        }

        // 3. 从云端拉取增量变更
        val lastSyncTimestamp = settingsRepository.lastSyncTime.first()
        val lastSyncIso = isoFormatter.format(Date(if (lastSyncTimestamp > 0) lastSyncTimestamp else 0L))
        val fetchResult = client.fetchUpdatedDiaries(lastSyncIso)

        if (fetchResult.isSuccess) {
            val remoteDiaries = fetchResult.getOrNull() ?: JsonArray(emptyList())
            for (element in remoteDiaries) {
                val obj = element.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: continue
                val remoteUpdatedAt = parseIsoToMillis(obj["updated_at"]?.jsonPrimitive?.content)

                // 用不过滤 isDeleted 的查询判断本地记录，避免软删除记录被误判为不存在
                val localDiary = diaryDao.getDiaryEntityAnyStatus(id)
                val localDeletedPending = localDiary?.syncStatus == 2 // 本地删除尚未推送

                if (localDeletedPending) {
                    // 本地删除意图优先：远端也已删除则标记已同步；否则保留本地删除待推送，不被覆盖
                    val remoteIsDeleted = obj["is_deleted"]?.jsonPrimitive?.booleanOrNull ?: false
                    if (remoteIsDeleted) {
                        diaryDao.updateSyncStatus(id, 0)
                    }
                    continue
                }

                if (localDiary == null || remoteUpdatedAt > localDiary.updatedAt) {
                    val remoteEntryDate = parseIsoToMillis(obj["entry_date"]?.jsonPrimitive?.content)
                    val remoteCreatedAt = parseIsoToMillis(obj["created_at"]?.jsonPrimitive?.content)
                    val isDeleted = obj["is_deleted"]?.jsonPrimitive?.booleanOrNull ?: false

                    // 本地对此记录毫无概念且云端已是删除状态 → 无需插入（防止无谓复活）
                    if (localDiary == null && isDeleted) continue

                    val mergedEntity = DiaryEntity(
                        id = id,
                        userId = obj["user_id"]?.jsonPrimitive?.content ?: "remote_user",
                        title = obj["title"]?.jsonPrimitive?.content ?: "",
                        contentMarkdown = obj["content_markdown"]?.jsonPrimitive?.content ?: "",
                        mood = obj["mood"]?.jsonPrimitive?.content ?: "CALM",
                        weather = obj["weather"]?.jsonPrimitive?.content ?: "SUNNY",
                        locationName = obj["location_name"]?.jsonPrimitive?.contentOrNull,
                        latitude = obj["latitude"]?.jsonPrimitive?.doubleOrNull,
                        longitude = obj["longitude"]?.jsonPrimitive?.doubleOrNull,
                        entryDate = if (remoteEntryDate > 0) remoteEntryDate else System.currentTimeMillis(),
                        createdAt = if (remoteCreatedAt > 0) remoteCreatedAt else System.currentTimeMillis(),
                        updatedAt = remoteUpdatedAt,
                        isPinned = obj["is_pinned"]?.jsonPrimitive?.booleanOrNull ?: false,
                        isDeleted = isDeleted,
                        deletedAt = if (isDeleted) System.currentTimeMillis() else null,
                        syncStatus = 0
                    )
                    diaryDao.insertOrUpdate(mergedEntity)
                    syncedCount++
                }
            }
        }

        // 更新最后同步时间
        val now = System.currentTimeMillis()
        settingsRepository.updateLastSyncTime(now)
        Result.success(syncedCount)
    }

    private fun parseIsoToMillis(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return 0L
        return try {
            isoFormatter.parse(isoString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
