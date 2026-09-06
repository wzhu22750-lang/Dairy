package com.example.inkpaperdiary.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inkpaperdiary.core.backup.BackupManager
import com.example.inkpaperdiary.core.backup.TxtDiaryImporter
import com.example.inkpaperdiary.core.designsystem.components.PaperPattern
import com.example.inkpaperdiary.core.network.SupabaseClient
import com.example.inkpaperdiary.core.sync.SyncManager
import com.example.inkpaperdiary.data.repository.DiaryRepository
import com.example.inkpaperdiary.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class SettingsUiState(
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val biometricEnabled: Boolean = false,
    val paperPattern: PaperPattern = PaperPattern.BLANK,
    val autoSyncEnabled: Boolean = false,
    val lastSyncTime: Long = 0L,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val exportFile: File? = null
)

private data class SupabaseSettings(
    val url: String,
    val anonKey: String,
    val autoSync: Boolean,
    val lastSync: Long
)

private data class SecuritySettings(
    val lockEnabled: Boolean,
    val pin: String,
    val bioEnabled: Boolean,
    val pattern: PaperPattern
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val diaryRepository: DiaryRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val supabaseFlow = combine(
        settingsRepository.supabaseUrl,
        settingsRepository.supabaseAnonKey,
        settingsRepository.autoSyncEnabled,
        settingsRepository.lastSyncTime
    ) { url, key, autoSync, lastSync ->
        SupabaseSettings(url, key, autoSync, lastSync)
    }

    private val securityFlow = combine(
        settingsRepository.appLockEnabled,
        settingsRepository.appLockPin,
        settingsRepository.biometricEnabled,
        settingsRepository.paperPattern
    ) { lockEnabled, pin, bioEnabled, pattern ->
        SecuritySettings(lockEnabled, pin, bioEnabled, pattern)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        supabaseFlow,
        securityFlow
    ) { supa, sec ->
        SettingsUiState(
            supabaseUrl = supa.url,
            supabaseAnonKey = supa.anonKey,
            appLockEnabled = sec.lockEnabled,
            appLockPin = sec.pin,
            biometricEnabled = sec.bioEnabled,
            paperPattern = sec.pattern,
            autoSyncEnabled = supa.autoSync,
            lastSyncTime = supa.lastSync
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun saveSupabaseConfig(url: String, anonKey: String) {
        viewModelScope.launch {
            settingsRepository.saveSupabaseConfig(url, anonKey)
            _syncMessage.value = "Supabase 配置已保存"
        }
    }

    fun testSupabaseConnection(url: String, anonKey: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val client = SupabaseClient(url, anonKey)
            val res = client.testConnection()
            if (res.isSuccess) {
                onResult(true, "连接成功！Supabase 凭据有效")
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "连接失败")
            }
        }
    }

    fun performManualSync() {
        viewModelScope.launch {
            _syncMessage.value = "正在同步..."
            val result = syncManager.performSync()
            if (result.isSuccess) {
                _syncMessage.value = "同步完成，已同步 ${result.getOrDefault(0)} 条变更"
            } else {
                _syncMessage.value = "同步失败: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSyncEnabled(enabled)
        }
    }

    fun setAppLock(enabled: Boolean, pin: String) {
        viewModelScope.launch {
            settingsRepository.setAppLock(enabled, pin)
        }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
        }
    }

    fun setPaperPattern(pattern: PaperPattern) {
        viewModelScope.launch {
            settingsRepository.setPaperPattern(pattern)
        }
    }

    fun exportMarkdownZip(context: Context, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            val diaries = diaryRepository.getAllDiaries().first()
            val res = BackupManager.exportToMarkdownZip(context, diaries)
            onResult(res.getOrNull())
        }
    }

    fun exportJsonBackup(context: Context, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            val diaries = diaryRepository.getAllDiaries().first()
            val res = BackupManager.exportToJson(context, diaries)
            onResult(res.getOrNull())
        }
    }

    fun importJsonBackup(jsonString: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val res = BackupManager.importFromJson(jsonString)
            val list = res.getOrNull() ?: emptyList()
            for (d in list) {
                diaryRepository.saveDiary(d)
            }
            onResult(list.size)
        }
    }

    fun importTxtFiles(context: Context, uris: List<Uri>, onResult: (importedCount: Int, failedCount: Int) -> Unit) {
        viewModelScope.launch {
            val (count, failed) = TxtDiaryImporter.importTxtUris(context, uris) { diary ->
                diaryRepository.saveDiary(diary)
            }
            onResult(count, failed)
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
