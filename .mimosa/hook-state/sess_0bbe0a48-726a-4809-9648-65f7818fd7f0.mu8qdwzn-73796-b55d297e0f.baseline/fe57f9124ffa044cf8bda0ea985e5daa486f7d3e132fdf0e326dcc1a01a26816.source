package com.example.inkpaperdiary.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inkpaperdiary.data.repository.DiaryRepository
import com.example.inkpaperdiary.domain.model.Diary
import kotlinx.coroutines.flow.*
import java.util.*

data class CalendarUiState(
    val selectedDate: Calendar = Calendar.getInstance(),
    val currentMonth: Calendar = Calendar.getInstance(),
    val monthDiaries: List<Diary> = emptyList(),
    val selectedDayDiaries: List<Diary> = emptyList(),
    val diaryDaysMap: Map<Int, List<Diary>> = emptyMap()
)

class CalendarViewModel(private val repository: DiaryRepository) : ViewModel() {

    private val _currentMonth = MutableStateFlow(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    })

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())

    val uiState: StateFlow<CalendarUiState> = combine(
        repository.getAllDiaries(),
        _currentMonth,
        _selectedDate
    ) { diaries, monthCal, selectedCal ->
        val cal = Calendar.getInstance()
        
        // 过滤出当月的日记
        val monthStart = monthCal.timeInMillis
        val monthEnd = (monthCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis

        val monthList = diaries.filter { it.entryDate in monthStart..monthEnd }

        // 按每月的几号（1..31）分组
        val daysMap = monthList.groupBy { diary ->
            cal.timeInMillis = diary.entryDate
            cal.get(Calendar.DAY_OF_MONTH)
        }

        // 选定日期的日记
        val selectedYear = selectedCal.get(Calendar.YEAR)
        val selectedDayOfYear = selectedCal.get(Calendar.DAY_OF_YEAR)
        val selectedList = diaries.filter { diary ->
            cal.timeInMillis = diary.entryDate
            cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.DAY_OF_YEAR) == selectedDayOfYear
        }

        CalendarUiState(
            selectedDate = selectedCal,
            currentMonth = monthCal,
            monthDiaries = monthList,
            selectedDayDiaries = selectedList,
            diaryDaysMap = daysMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun selectDate(dayOfMonth: Int) {
        val newCal = (_currentMonth.value.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        _selectedDate.value = newCal
    }

    fun nextMonth() {
        val newMonth = (_currentMonth.value.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        _currentMonth.value = newMonth
    }

    fun previousMonth() {
        val newMonth = (_currentMonth.value.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        _currentMonth.value = newMonth
    }
}
