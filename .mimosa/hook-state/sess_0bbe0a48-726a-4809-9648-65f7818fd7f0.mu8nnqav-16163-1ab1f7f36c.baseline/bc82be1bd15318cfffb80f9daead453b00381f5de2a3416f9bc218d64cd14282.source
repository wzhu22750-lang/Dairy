package com.example.inkpaperdiary.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inkpaperdiary.data.repository.DiaryRepository
import com.example.inkpaperdiary.domain.model.Diary
import com.example.inkpaperdiary.domain.model.Mood
import com.example.inkpaperdiary.domain.model.Tag
import com.example.inkpaperdiary.domain.model.Weather
import kotlinx.coroutines.flow.*

data class SearchUiState(
    val query: String = "",
    val results: List<Diary> = emptyList(),
    val selectedMood: Mood? = null,
    val selectedWeather: Weather? = null,
    val selectedTag: Tag? = null,
    val allTags: List<Tag> = emptyList()
)

private data class SearchFilters(
    val mood: Mood? = null,
    val weather: Weather? = null,
    val tag: Tag? = null
)

class SearchViewModel(private val repository: DiaryRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _selectedMood = MutableStateFlow<Mood?>(null)
    private val _selectedWeather = MutableStateFlow<Weather?>(null)
    private val _selectedTag = MutableStateFlow<Tag?>(null)

    private val filtersFlow = combine(
        _selectedMood,
        _selectedWeather,
        _selectedTag
    ) { mood, weather, tag ->
        SearchFilters(mood, weather, tag)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        filtersFlow,
        repository.getAllDiaries(),
        repository.getAllTags()
    ) { query, filters, allDiaries, allTags ->
        val trimmedQuery = query.trim()
        var filtered = allDiaries

        if (trimmedQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(trimmedQuery, ignoreCase = true) ||
                        it.contentMarkdown.contains(trimmedQuery, ignoreCase = true) ||
                        (it.locationName?.contains(trimmedQuery, ignoreCase = true) == true)
            }
        }

        if (filters.mood != null) {
            filtered = filtered.filter { it.mood == filters.mood }
        }

        if (filters.weather != null) {
            filtered = filtered.filter { it.weather == filters.weather }
        }

        if (filters.tag != null) {
            val filterTag = filters.tag
            filtered = filtered.filter { diary -> diary.tags.any { it.id == filterTag.id || it.name == filterTag.name } }
        }

        SearchUiState(
            query = query,
            results = if (trimmedQuery.isBlank() && filters.mood == null && filters.weather == null && filters.tag == null) emptyList() else filtered,
            selectedMood = filters.mood,
            selectedWeather = filters.weather,
            selectedTag = filters.tag,
            allTags = allTags
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun toggleMood(mood: Mood) {
        _selectedMood.value = if (_selectedMood.value == mood) null else mood
    }

    fun toggleWeather(weather: Weather) {
        _selectedWeather.value = if (_selectedWeather.value == weather) null else weather
    }

    fun toggleTag(tag: Tag) {
        _selectedTag.value = if (_selectedTag.value?.id == tag.id) null else tag
    }
}
