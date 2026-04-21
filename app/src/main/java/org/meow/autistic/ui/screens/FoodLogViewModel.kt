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
import org.meow.autistic.data.foodlog.FoodLogEntry
import org.meow.autistic.data.foodlog.FoodLogItemEntry
import org.meow.autistic.data.foodlog.FoodLogRepository
import java.time.LocalDate

/**
 * ViewModel for the food log daily tracker.
 * Individual [FoodLogItemEntry] records are summed to produce daily nutrition totals.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FoodLogViewModel(private val repository: FoodLogRepository) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now().toString())
    val date: StateFlow<String> = _date.asStateFlow()

    val items: StateFlow<List<FoodLogItemEntry>> = _date
        .flatMapLatest { repository.getItemsByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totals: StateFlow<FoodLogEntry> = combine(_date, items) { date, list ->
        FoodLogEntry(
            date = date,
            calories = list.sumOf { it.calories },
            protein = list.sumOf { it.protein },
            totalFat = list.sumOf { it.totalFat },
            totalCarbs = list.sumOf { it.totalCarbs },
            fiber = list.sumOf { it.fiber },
            totalSugars = list.sumOf { it.totalSugars },
            addedSugars = list.sumOf { it.addedSugars },
            sugarAlcohols = list.sumOf { it.sugarAlcohols },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FoodLogEntry(date = LocalDate.now().toString()),
    )

    fun previousDay() { _date.value = LocalDate.parse(_date.value).minusDays(1).toString() }
    fun nextDay() { _date.value = LocalDate.parse(_date.value).plusDays(1).toString() }

    fun addItem(item: FoodLogItemEntry) {
        viewModelScope.launch {
            repository.insertItem(item.copy(id = 0, date = _date.value, loggedAt = java.time.Instant.now()))
        }
    }

    fun deleteItem(item: FoodLogItemEntry) {
        viewModelScope.launch { repository.deleteItem(item) }
    }
}
