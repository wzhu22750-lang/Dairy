package com.example.inkpaperdiary

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.inkpaperdiary.core.database.AppDatabase
import com.example.inkpaperdiary.core.designsystem.PaperDiaryTheme
import com.example.inkpaperdiary.core.security.AppLockManager
import com.example.inkpaperdiary.core.sync.SyncManager
import com.example.inkpaperdiary.core.sync.SyncWorker
import com.example.inkpaperdiary.data.repository.DiaryRepository
import com.example.inkpaperdiary.data.repository.MediaRepository
import com.example.inkpaperdiary.data.repository.SettingsRepository
import com.example.inkpaperdiary.ui.navigation.AppNavigation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var diaryRepository: DiaryRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var mediaRepository: MediaRepository
    private lateinit var syncManager: SyncManager

    private var isLockEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getInstance(this)
        diaryRepository = DiaryRepository(database)
        settingsRepository = SettingsRepository(this)
        mediaRepository = MediaRepository(this)
        syncManager = SyncManager(database, settingsRepository)

        // 检查并调度后台同步
        lifecycleScope.launch {
            val autoSync = settingsRepository.autoSyncEnabled.first()
            if (autoSync) {
                SyncWorker.schedulePeriodicSync(this@MainActivity)
            }

            // 启动时清理 30 天前已删除的日记（含附件文件），兑现回收站"30 天内可恢复"承诺
            runCatching { diaryRepository.purgeOldTrash(30) }
        }

        // 动态监听应用锁开关：若开启则开启多任务防窥保护 FLAG_SECURE，关闭时移除
        lifecycleScope.launch {
            settingsRepository.appLockEnabled.collect { enabled ->
                isLockEnabled = enabled
                if (enabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        setContent {
            val appLockActive by settingsRepository.appLockEnabled.collectAsState(initial = false)
            val readingSettings by settingsRepository.readingSettings.collectAsState(initial = com.example.inkpaperdiary.core.designsystem.ReadingSettings())

            val darkTheme = when (readingSettings.themeMode) {
                com.example.inkpaperdiary.core.designsystem.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                com.example.inkpaperdiary.core.designsystem.ThemeMode.LIGHT -> false
                com.example.inkpaperdiary.core.designsystem.ThemeMode.DARK -> true
            }

            PaperDiaryTheme(darkTheme = darkTheme, reading = readingSettings) {
                AppNavigation(
                    diaryRepository = diaryRepository,
                    settingsRepository = settingsRepository,
                    mediaRepository = mediaRepository,
                    syncManager = syncManager,
                    isLockEnabled = appLockActive
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // 只要退出了界面（按 Home、切换应用、息屏），且不是启动系统级图片/文件选择器，下一次进入应用即刻重新触发锁屏
        if (isLockEnabled && !AppLockManager.isPickerActive) {
            AppLockManager.lock()
        }
    }

    override fun onResume() {
        super.onResume()
        // 恢复前台后重置外部选择器状态
        AppLockManager.isPickerActive = false
    }
}
