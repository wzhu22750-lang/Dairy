package com.example.inkpaperdiary.ui.timeline

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inkpaperdiary.core.backup.TxtDiaryImporter
import com.example.inkpaperdiary.data.repository.DiaryRepository
import com.example.inkpaperdiary.domain.model.Diary
import com.example.inkpaperdiary.domain.model.Mood
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TimelineUiState(
    val diaries: List<Diary> = emptyList(),
    val filteredDiaries: List<Diary> = emptyList(),
    val selectedMoodFilter: Mood? = null,
    val onlyPinned: Boolean = false,
    val isLoading: Boolean = false
)

class TimelineViewModel(private val repository: DiaryRepository) : ViewModel() {

    private val _selectedMood = MutableStateFlow<Mood?>(null)
    private val _onlyPinned = MutableStateFlow(false)

    val uiState: StateFlow<TimelineUiState> = combine(
        repository.getAllDiaries(),
        _selectedMood,
        _onlyPinned
    ) { diaries, moodFilter, onlyPinned ->
        var filtered = diaries
        if (moodFilter != null) {
            filtered = filtered.filter { it.mood == moodFilter }
        }
        if (onlyPinned) {
            filtered = filtered.filter { it.isPinned }
        }
        TimelineUiState(
            diaries = diaries,
            filteredDiaries = filtered,
            selectedMoodFilter = moodFilter,
            onlyPinned = onlyPinned,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimelineUiState(isLoading = true))

    fun setMoodFilter(mood: Mood?) {
        _selectedMood.value = if (_selectedMood.value == mood) null else mood
    }

    fun togglePinnedFilter() {
        _onlyPinned.value = !_onlyPinned.value
    }

    fun togglePin(diary: Diary) {
        viewModelScope.launch {
            repository.togglePin(diary.id)
        }
    }

    fun deleteDiary(diaryId: String) {
        viewModelScope.launch {
            repository.softDeleteDiary(diaryId)
        }
    }

    fun importTxtFiles(context: Context, uris: List<Uri>, onResult: (importedCount: Int, failedCount: Int) -> Unit) {
        viewModelScope.launch {
            val (count, failed) = TxtDiaryImporter.importTxtUris(context, uris) { diary ->
                repository.saveDiary(diary)
            }
            onResult(count, failed)
        }
    }
}
