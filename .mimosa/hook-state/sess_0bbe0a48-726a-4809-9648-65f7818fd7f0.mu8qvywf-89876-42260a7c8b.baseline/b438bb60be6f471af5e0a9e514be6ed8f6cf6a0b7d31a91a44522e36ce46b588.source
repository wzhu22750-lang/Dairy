package com.example.inkpaperdiary.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inkpaperdiary.data.repository.DiaryRepository
import com.example.inkpaperdiary.domain.model.Stats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(private val repository: DiaryRepository) : ViewModel() {
    val stats: StateFlow<Stats> = repository.getStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Stats())
}
