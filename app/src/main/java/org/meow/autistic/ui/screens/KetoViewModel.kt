package org.meow.autistic.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meow.autistic.data.keto.KetoLogEntry
import org.meow.autistic.data.keto.KetoRepository
import java.time.LocalDate

/** Fields that can be incremented or decremented on the keto screen. */
enum class KetoField {
    TOTAL_CARBS, FIBER, TOTAL_SUGARS, ADDED_SUGARS, SUGAR_ALCOHOLS
}

/**
 * ViewModel for the keto daily tracker. Holds a single [KetoLogEntry] per day;
 * [adjust] increments or decrements one field and upserts the result.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KetoViewModel(private val repository: KetoRepository) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now().toString())
    val date: StateFlow<String> = _date.asStateFlow()

    val entry: StateFlow<KetoLogEntry> = _date
        .flatMapLatest { date -> repository.getByDate(date).map { it ?: KetoLogEntry(date = date) } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            KetoLogEntry(date = LocalDate.now().toString()),
        )

    fun previousDay() { _date.value = LocalDate.parse(_date.value).minusDays(1).toString() }
    fun nextDay() { _date.value = LocalDate.parse(_date.value).plusDays(1).toString() }

    fun adjust(field: KetoField, delta: Double) {
        viewModelScope.launch {
            val e = entry.value
            val updated = when (field) {
                KetoField.TOTAL_CARBS -> e.copy(totalCarbs = (e.totalCarbs + delta).coerceAtLeast(0.0))
                KetoField.FIBER -> e.copy(fiber = (e.fiber + delta).coerceAtLeast(0.0))
                KetoField.TOTAL_SUGARS -> e.copy(totalSugars = (e.totalSugars + delta).coerceAtLeast(0.0))
                KetoField.ADDED_SUGARS -> e.copy(addedSugars = (e.addedSugars + delta).coerceAtLeast(0.0))
                KetoField.SUGAR_ALCOHOLS -> e.copy(sugarAlcohols = (e.sugarAlcohols + delta).coerceAtLeast(0.0))
            }
            repository.upsert(updated)
        }
    }
}
