package org.meow.autistic.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meow.autistic.data.keto.KetoLogEntry
import org.meow.autistic.data.keto.KetoRepository
import java.time.LocalDate

/**
 * ViewModel for the keto daily tracker. Exposes entries for the currently viewed date
 * and supports navigating to adjacent days.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KetoViewModel(private val repository: KetoRepository) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now().toString())
    val date: StateFlow<String> = _date.asStateFlow()

    val entries: StateFlow<List<KetoLogEntry>> = _date
        .flatMapLatest { repository.getByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun previousDay() { _date.value = LocalDate.parse(_date.value).minusDays(1).toString() }
    fun nextDay() { _date.value = LocalDate.parse(_date.value).plusDays(1).toString() }

    fun insert(entry: KetoLogEntry) = viewModelScope.launch { repository.insert(entry) }
    fun delete(entry: KetoLogEntry) = viewModelScope.launch { repository.delete(entry) }
}
