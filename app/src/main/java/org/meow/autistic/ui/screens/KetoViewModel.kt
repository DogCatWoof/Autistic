package org.meow.autistic.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meow.autistic.data.keto.KetoItemEntry
import org.meow.autistic.data.keto.KetoLogEntry
import org.meow.autistic.data.keto.KetoRepository
import java.time.LocalDate

/**
 * ViewModel for the keto daily tracker.
 * Individual [KetoItemEntry] records are summed to produce daily nutrition totals.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KetoViewModel(private val repository: KetoRepository) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now().toString())
    val date: StateFlow<String> = _date.asStateFlow()

    val items: StateFlow<List<KetoItemEntry>> = _date
        .flatMapLatest { repository.getItemsByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totals: StateFlow<KetoLogEntry> = combine(_date, items) { date, list ->
        KetoLogEntry(
            date = date,
            calories = list.sumOf { it.calories },
            totalCarbs = list.sumOf { it.totalCarbs },
            fiber = list.sumOf { it.fiber },
            totalSugars = list.sumOf { it.totalSugars },
            addedSugars = list.sumOf { it.addedSugars },
            sugarAlcohols = list.sumOf { it.sugarAlcohols },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        KetoLogEntry(date = LocalDate.now().toString()),
    )

    fun previousDay() { _date.value = LocalDate.parse(_date.value).minusDays(1).toString() }
    fun nextDay() { _date.value = LocalDate.parse(_date.value).plusDays(1).toString() }

    fun addItem(item: KetoItemEntry) {
        viewModelScope.launch {
            repository.insertItem(item.copy(id = 0, date = _date.value, loggedAt = java.time.Instant.now()))
        }
    }

    fun deleteItem(item: KetoItemEntry) {
        viewModelScope.launch { repository.deleteItem(item) }
    }
}
