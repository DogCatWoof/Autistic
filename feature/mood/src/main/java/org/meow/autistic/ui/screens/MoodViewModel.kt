package org.meow.autistic.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meow.autistic.data.mood.MoodEntity
import org.meow.autistic.data.mood.MoodRepository

/** ViewModel for the Mood screen. Exposes the list of mood readings. */
class MoodViewModel(private val repository: MoodRepository) : ViewModel() {

    val moods: StateFlow<List<MoodEntity>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insert(mood: MoodEntity) = viewModelScope.launch { repository.insert(mood) }
    fun delete(mood: MoodEntity) = viewModelScope.launch { repository.delete(mood) }
}
