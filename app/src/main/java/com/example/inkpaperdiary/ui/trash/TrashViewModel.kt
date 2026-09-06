package com.example.inkpaperdiary.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inkpaperdiary.data.repository.DiaryRepository
import com.example.inkpaperdiary.domain.model.Diary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(private val repository: DiaryRepository) : ViewModel() {
    val trashDiaries: StateFlow<List<Diary>> = repository.getTrashDiaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restore(diaryId: String) {
        viewModelScope.launch {
            repository.restoreDiary(diaryId)
        }
    }

    fun permanentDelete(diaryId: String) {
        viewModelScope.launch {
            repository.hardDeleteDiary(diaryId)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.purgeOldTrash(maxDays = 0)
        }
    }
}
