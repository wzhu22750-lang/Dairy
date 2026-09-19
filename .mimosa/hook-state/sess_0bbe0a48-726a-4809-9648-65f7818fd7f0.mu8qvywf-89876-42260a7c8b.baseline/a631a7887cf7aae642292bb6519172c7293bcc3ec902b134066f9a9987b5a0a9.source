package com.example.inkpaperdiary.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inkpaperdiary.core.designsystem.ReaderFont
import com.example.inkpaperdiary.core.designsystem.ReadingSettings
import com.example.inkpaperdiary.core.designsystem.ThemeMode
import com.example.inkpaperdiary.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 阅读排版设置：像 Kindle 一样，调整是全局且持久的。
 */
class ReadingSettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<ReadingSettings> = settingsRepository.readingSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingSettings())

    fun setFont(font: ReaderFont) = launch { settingsRepository.setReaderFont(font) }

    fun increaseFontScale() = launch { settingsRepository.setReaderFontScale(settings.value.fontScale + 0.1f) }

    fun decreaseFontScale() = launch { settingsRepository.setReaderFontScale(settings.value.fontScale - 0.1f) }

    fun setLineSpacing(spacing: Float) = launch { settingsRepository.setReaderLineSpacing(spacing) }

    fun setPageWidth(width: Float) = launch { settingsRepository.setReaderPageWidth(width) }

    fun setThemeMode(mode: ThemeMode) = launch { settingsRepository.setThemeMode(mode) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
