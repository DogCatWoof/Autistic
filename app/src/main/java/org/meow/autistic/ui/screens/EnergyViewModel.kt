package org.meow.autistic.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meow.autistic.data.debug.ExceptionReporter
import org.meow.autistic.data.energy.EnergyLogEntry
import org.meow.autistic.data.energy.EnergyProfileEntity
import org.meow.autistic.data.energy.EnergyRepository
import org.meow.autistic.data.energy.StartOfDayEntry
import java.time.LocalDate

/**
 * ViewModel for the Energy Budgeting screen.
 * Exposes today's capacity, balance, log entries, and start-of-day check-in state.
 */
class EnergyViewModel(
    private val repository: EnergyRepository,
    private val exceptionReporter: ExceptionReporter,
) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, e -> exceptionReporter.report(e) }

    val todayLogs: StateFlow<List<EnergyLogEntry>> = repository.getTodayLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _profile = MutableStateFlow<EnergyProfileEntity?>(null)
    val profile: StateFlow<EnergyProfileEntity?> = _profile.asStateFlow()

    private val _todayCapacity = MutableStateFlow(10.0)
    val todayCapacity: StateFlow<Double> = _todayCapacity.asStateFlow()

    private val _todayBalance = MutableStateFlow(10.0)
    val todayBalance: StateFlow<Double> = _todayBalance.asStateFlow()

    private val _startOfDay = MutableStateFlow<StartOfDayEntry?>(null)
    val startOfDay: StateFlow<StartOfDayEntry?> = _startOfDay.asStateFlow()

    private val _projectedBalance = MutableStateFlow<Double?>(null)
    val projectedBalance: StateFlow<Double?> = _projectedBalance.asStateFlow()

    init {
        viewModelScope.launch(handler) {
            _profile.value = repository.getOrCreateProfile()
            _startOfDay.value = repository.getTodayStartOfDay()
            refreshBalance()
        }
        todayLogs.onEach { refreshBalance() }.launchIn(viewModelScope)
    }

    private suspend fun refreshBalance() {
        _todayCapacity.value = repository.getTodayCapacity()
        _todayBalance.value = repository.getTodayBalance()
    }

    fun logActivity(activityType: String, description: String?, difficulty: Int?) {
        viewModelScope.launch(handler) {
            repository.logActivity(activityType, description, difficulty)
        }
    }

    fun deleteLog(entry: EnergyLogEntry) {
        viewModelScope.launch(handler) { repository.deleteLog(entry) }
    }

    fun submitStartOfDay(sleepQuality: Int, stressLevel: Int, physicalState: Int) {
        viewModelScope.launch(handler) {
            val score = (sleepQuality + physicalState + (6 - stressLevel)) / 15.0
            val multiplier = (0.6 + score * 0.6).coerceIn(0.6, 1.2)
            val entry = StartOfDayEntry(
                date = LocalDate.now().toString(),
                sleepQuality = sleepQuality,
                stressLevel = stressLevel,
                physicalState = physicalState,
                baselineMultiplier = multiplier,
            )
            repository.upsertStartOfDay(entry)
            _startOfDay.value = entry
            refreshBalance()
        }
    }

    fun previewProjection(activityType: String) {
        viewModelScope.launch(handler) {
            _projectedBalance.value = repository.projectBalance(activityType)
        }
    }

    fun clearProjection() { _projectedBalance.value = null }

    fun updateProfile(dailyCapacity: Int) {
        viewModelScope.launch(handler) {
            val updated = (_profile.value ?: EnergyProfileEntity()).copy(dailyCapacity = dailyCapacity)
            repository.updateProfile(updated)
            _profile.value = updated
            refreshBalance()
        }
    }

    fun costFor(activityType: String): Double =
        EnergyRepository.DEFAULT_COSTS[activityType] ?: 2.0
}
