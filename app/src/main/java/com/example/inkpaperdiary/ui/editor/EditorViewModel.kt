package com.example.inkpaperdiary.ui.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inkpaperdiary.data.repository.DiaryRepository
import com.example.inkpaperdiary.data.repository.MediaRepository
import com.example.inkpaperdiary.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class EditorUiState(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "local_guest",
    val title: String = "",
    val contentMarkdown: String = "",
    val mood: Mood = Mood.CALM,
    val weather: Weather = Weather.SUNNY,
    val locationName: String? = null,
    val entryDate: Long = System.currentTimeMillis(),
    val createdAt: Long = 0L,
    val tags: List<Tag> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val isPinned: Boolean = false,
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false
)

class EditorViewModel(
    private val diaryRepository: DiaryRepository,
    private val mediaRepository: MediaRepository,
    private val diaryId: String?,
    private val initialEntryDate: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        loadDiary()
    }

    private fun loadDiary() {
        if (diaryId == null || diaryId == "new") {
            _uiState.update {
                it.copy(
                    isLoaded = true,
                    // 支持"补写某天"：新建日记时用目标日期而不是当前时间
                    entryDate = initialEntryDate ?: System.currentTimeMillis()
                )
            }
            return
        }
        viewModelScope.launch {
            val existing = diaryRepository.getDiaryById(diaryId)
            if (existing != null) {
                _uiState.value = EditorUiState(
                    id = existing.id,
                    userId = existing.userId,
                    title = existing.title,
                    contentMarkdown = existing.contentMarkdown,
                    mood = existing.mood,
                    weather = existing.weather,
                    locationName = existing.locationName,
                    entryDate = existing.entryDate,
                    createdAt = existing.createdAt,
                    tags = existing.tags,
                    attachments = existing.attachments,
                    isPinned = existing.isPinned,
                    isLoaded = true
                )
            } else {
                _uiState.update { it.copy(isLoaded = true) }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(contentMarkdown = content) }
    }

    fun updateMood(mood: Mood) {
        _uiState.update { it.copy(mood = mood) }
    }

    fun updateWeather(weather: Weather) {
        _uiState.update { it.copy(weather = weather) }
    }

    fun updateLocation(location: String?) {
        _uiState.update { it.copy(locationName = location) }
    }

    fun updateEntryDate(timestamp: Long) {
        _uiState.update { it.copy(entryDate = timestamp) }
    }

    fun togglePinned() {
        _uiState.update { it.copy(isPinned = !it.isPinned) }
    }

    fun addTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val currentTags = _uiState.value.tags
        if (currentTags.any { it.name.equals(trimmed, ignoreCase = true) }) return
        val newTag = Tag(id = UUID.randomUUID().toString(), name = trimmed)
        _uiState.update { it.copy(tags = currentTags + newTag) }
    }

    fun removeTag(tagId: String) {
        _uiState.update { it.copy(tags = it.tags.filterNot { tag -> tag.id == tagId }) }
    }

    fun addImage(uri: Uri) {
        viewModelScope.launch {
            val attachment = mediaRepository.saveImageFromUri(uri, _uiState.value.id)
            if (attachment != null) {
                _uiState.update { it.copy(attachments = it.attachments + attachment) }
            }
        }
    }

    fun removeAttachment(attachmentId: String) {
        _uiState.update { it.copy(attachments = it.attachments.filterNot { att -> att.id == attachmentId }) }
    }

    fun saveDiary(onSaved: () -> Unit) {
        val state = _uiState.value
        // 如果标题和正文都为空且没有图片，则不保存
        if (state.title.isBlank() && state.contentMarkdown.isBlank() && state.attachments.isEmpty()) {
            onSaved()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val diary = Diary(
                id = state.id,
                userId = state.userId,
                title = state.title.trim(),
                contentMarkdown = state.contentMarkdown,
                mood = state.mood,
                weather = state.weather,
                locationName = state.locationName?.trim(),
                entryDate = state.entryDate,
                createdAt = state.createdAt, // 保留原创建时间，编辑不重置
                tags = state.tags,
                attachments = state.attachments,
                isPinned = state.isPinned
            )
            diaryRepository.saveDiary(diary)
            _uiState.update { it.copy(isSaving = false) }
            onSaved()
        }
    }
}