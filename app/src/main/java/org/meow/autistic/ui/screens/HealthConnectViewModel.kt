package org.meow.autistic.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meow.autistic.data.debug.ExceptionReporter
import org.meow.autistic.data.health.HealthConnectRepository
import org.meow.autistic.data.health.HealthSnapshotEntity

/** Exposes Health Connect status and 7-day snapshot history. */
class HealthConnectViewModel(
    private val repository: HealthConnectRepository,
    private val exceptionReporter: ExceptionReporter,
) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, e -> exceptionReporter.report(e) }

    private val _sdkStatus = MutableStateFlow(repository.getSdkStatus())
    val sdkStatus: StateFlow<Int> = _sdkStatus.asStateFlow()

    private val _grantedPermissions = MutableStateFlow<Set<String>>(emptySet())
    val grantedPermissions: StateFlow<Set<String>> = _grantedPermissions.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val recentSnapshots: StateFlow<List<HealthSnapshotEntity>> = repository
        .getRecentSnapshots(7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val requiredPermissions: Set<String> = repository.requiredPermissions

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        viewModelScope.launch(handler) {
            _sdkStatus.value = repository.getSdkStatus()
            _grantedPermissions.value = repository.getGrantedPermissions()
        }
    }

    fun refreshSnapshot() {
        viewModelScope.launch(handler) {
            _isRefreshing.value = true
            repository.refreshTodaySnapshot()
            _isRefreshing.value = false
        }
    }
}
