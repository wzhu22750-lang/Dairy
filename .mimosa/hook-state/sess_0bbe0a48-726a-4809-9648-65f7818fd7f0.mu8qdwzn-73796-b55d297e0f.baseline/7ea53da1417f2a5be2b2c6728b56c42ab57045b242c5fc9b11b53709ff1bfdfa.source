package com.example.inkpaperdiary.core.sync

import android.content.Context
import androidx.work.*
import com.example.inkpaperdiary.core.database.AppDatabase
import com.example.inkpaperdiary.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getInstance(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)

        // 用户已关闭自动同步时，直接跳过（防止遗留的周期任务继续同步）
        val autoSync = runCatching { settingsRepository.autoSyncEnabled.first() }.getOrDefault(false)
        if (!autoSync) {
            return Result.success()
        }

        val syncManager = SyncManager(database, settingsRepository)
        if (!syncManager.isConfigured()) {
            // 未配置 Supabase 属于永久性失败，重试毫无意义
            return Result.success()
        }

        val result = syncManager.performSync()
        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val SYNC_WORK_NAME = "ink_paper_diary_sync_work"

        fun schedulePeriodicSync(context: Context, wifiOnly: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                syncRequest
            )
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
        }

        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeRequest)
        }
    }
}