package com.example.inkpaperdiary.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.inkpaperdiary.core.designsystem.ReaderFont
import com.example.inkpaperdiary.core.designsystem.ReadingSettings
import com.example.inkpaperdiary.core.designsystem.ThemeMode
import com.example.inkpaperdiary.core.designsystem.components.PaperPattern
import com.example.inkpaperdiary.core.security.PinCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "diary_settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val KEY_SUPABASE_URL = stringPreferencesKey("supabase_url")
        val KEY_SUPABASE_ANON_KEY = stringPreferencesKey("supabase_anon_key")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val KEY_APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_PAPER_PATTERN = stringPreferencesKey("paper_pattern")
        val KEY_AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_timestamp")

        // ---- 阅读排版（Dairy 2.0）----
        val KEY_READER_FONT = stringPreferencesKey("reader_font")
        val KEY_READER_FONT_SCALE = floatPreferencesKey("reader_font_scale")
        val KEY_READER_LINE_SPACING = floatPreferencesKey("reader_line_spacing")
        val KEY_READER_PAGE_WIDTH = floatPreferencesKey("reader_page_width")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val supabaseUrl: Flow<String> = context.dataStore.data.safeCatch().map { it[KEY_SUPABASE_URL] ?: "" }
    val supabaseAnonKey: Flow<String> = context.dataStore.data.safeCatch().map { it[KEY_SUPABASE_ANON_KEY] ?: "" }
    val userEmail: Flow<String> = context.dataStore.data.safeCatch().map { it[KEY_USER_EMAIL] ?: "" }
    val appLockEnabled: Flow<Boolean> = context.dataStore.data.safeCatch().map { it[KEY_APP_LOCK_ENABLED] ?: false }
    val appLockPin: Flow<String> = context.dataStore.data.safeCatch().map { it[KEY_APP_LOCK_PIN] ?: "" }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.safeCatch().map { it[KEY_BIOMETRIC_ENABLED] ?: false }
    val paperPattern: Flow<PaperPattern> = context.dataStore.data.safeCatch().map { 
        val name = it[KEY_PAPER_PATTERN] ?: PaperPattern.BLANK.name
        try { PaperPattern.valueOf(name) } catch (e: Exception) { PaperPattern.BLANK }
    }
    val autoSyncEnabled: Flow<Boolean> = context.dataStore.data.safeCatch().map { it[KEY_AUTO_SYNC_ENABLED] ?: false }
    val lastSyncTime: Flow<Long> = context.dataStore.data.safeCatch().map { it[KEY_LAST_SYNC_TIME] ?: 0L }

    // ---- 阅读排版 ----
    val readerFont: Flow<ReaderFont> = context.dataStore.data.safeCatch().map {
        val name = it[KEY_READER_FONT] ?: ReaderFont.SERIF.name
        runCatching { ReaderFont.valueOf(name) }.getOrDefault(ReaderFont.SERIF)
    }
    val readerFontScale: Flow<Float> = context.dataStore.data.safeCatch().map {
        (it[KEY_READER_FONT_SCALE] ?: 1f).coerceIn(0.8f, 1.4f)
    }
    val readerLineSpacing: Flow<Float> = context.dataStore.data.safeCatch().map {
        (it[KEY_READER_LINE_SPACING] ?: 1.75f).coerceIn(1.4f, 2.2f)
    }
    val readerPageWidth: Flow<Float> = context.dataStore.data.safeCatch().map {
        (it[KEY_READER_PAGE_WIDTH] ?: 1f).coerceIn(0.75f, 1f)
    }
    val themeMode: Flow<ThemeMode> = context.dataStore.data.safeCatch().map {
        val name = it[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    /** 聚合的阅读排版设置，供主题与阅读页消费。 */
    val readingSettings: Flow<ReadingSettings> = combine(
        readerFont, readerFontScale, readerLineSpacing, readerPageWidth, themeMode
    ) { font, scale, spacing, width, mode ->
        ReadingSettings(font = font, fontScale = scale, lineSpacing = spacing, pageWidth = width, themeMode = mode)
    }

    suspend fun setReaderFont(font: ReaderFont) {
        context.dataStore.edit { it[KEY_READER_FONT] = font.name }
    }

    suspend fun setReaderFontScale(scale: Float) {
        context.dataStore.edit { it[KEY_READER_FONT_SCALE] = scale.coerceIn(0.8f, 1.4f) }
    }

    suspend fun setReaderLineSpacing(spacing: Float) {
        context.dataStore.edit { it[KEY_READER_LINE_SPACING] = spacing.coerceIn(1.4f, 2.2f) }
    }

    suspend fun setReaderPageWidth(width: Float) {
        context.dataStore.edit { it[KEY_READER_PAGE_WIDTH] = width.coerceIn(0.75f, 1f) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun saveSupabaseConfig(url: String, anonKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SUPABASE_URL] = url.trim()
            prefs[KEY_SUPABASE_ANON_KEY] = anonKey.trim()
        }
    }

    suspend fun saveUserEmail(email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_EMAIL] = email.trim()
        }
    }

    suspend fun setAppLock(enabled: Boolean, pin: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
            // PIN 用 Keystore AES-GCM 加密后落盘，杜绝明文存储
            prefs[KEY_APP_LOCK_PIN] = if (enabled && pin.isNotBlank()) {
                PinCipher.encrypt(pin)
            } else {
                ""
            }
        }
    }

    /** 校验输入 PIN 是否正确；兼容旧版本明文存储的 PIN，验证通过后自动迁移为密文。 */
    suspend fun verifyAppPin(input: String): Boolean {
        val stored = appLockPin.first()
        if (stored.isBlank()) return false

        val plain = PinCipher.decrypt(stored)
        if (plain != null) return plain == input

        // 旧版本明文兼容：直接比较，成功后升级为密文
        if (stored == input) {
            context.dataStore.edit { prefs -> prefs[KEY_APP_LOCK_PIN] = PinCipher.encrypt(input) }
            return true
        }
        return false
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setPaperPattern(pattern: PaperPattern) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PAPER_PATTERN] = pattern.name
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_SYNC_ENABLED] = enabled
        }
    }

    suspend fun updateLastSyncTime(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_TIME] = timestamp
        }
    }
}

private fun Flow<Preferences>.safeCatch(): Flow<Preferences> = this.catch { exception ->
    if (exception is IOException) {
        emit(emptyPreferences())
    } else {
        throw exception
    }
}
