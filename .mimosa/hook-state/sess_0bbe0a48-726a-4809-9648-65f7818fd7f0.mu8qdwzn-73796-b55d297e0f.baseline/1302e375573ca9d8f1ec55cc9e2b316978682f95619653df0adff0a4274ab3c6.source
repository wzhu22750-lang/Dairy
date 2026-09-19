package com.example.inkpaperdiary.ui.onthisday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inkpaperdiary.data.repository.DiaryRepository
import com.example.inkpaperdiary.domain.model.Diary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

data class HistoricalMemory(
    val yearsAgo: Int,
    val diary: Diary
)

data class OnThisDayUiState(
    val memories: List<HistoricalMemory> = emptyList(),
    val isLoading: Boolean = true
)

class OnThisDayViewModel(private val repository: DiaryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OnThisDayUiState())
    val uiState: StateFlow<OnThisDayUiState> = _uiState.asStateFlow()

    init {
        loadMemories()
    }

    fun loadMemories() {
        viewModelScope.launch {
            _uiState.value = OnThisDayUiState(isLoading = true)
            val allOnThisDay = repository.getOnThisDay(Date())
            
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val cal = Calendar.getInstance()

            val memories = allOnThisDay
                .filter {
                    cal.timeInMillis = it.entryDate
                    cal.get(Calendar.YEAR) < currentYear
                }
                .map { diary ->
                    cal.timeInMillis = diary.entryDate
                    val diaryYear = cal.get(Calendar.YEAR)
                    val yearsAgo = currentYear - diaryYear
                    HistoricalMemory(yearsAgo = yearsAgo, diary = diary)
                }
                .sortedBy { it.yearsAgo }

            _uiState.value = OnThisDayUiState(memories = memories, isLoading = false)
        }
    }
}
