package org.meow.autistic.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.meow.autistic.data.product.OFF_SYNC_WORK_NAME
import org.meow.autistic.data.product.OpenFoodFactsWorker
import org.meow.autistic.data.product.ProductRepository

sealed class ScanUiState {
    object Loading : ScanUiState()
    object NeedsSync : ScanUiState()
    data class Syncing(val status: String) : ScanUiState()
    object Scanning : ScanUiState()
    data class Found(val productJson: String) : ScanUiState()
    data class NotFound(val barcode: String) : ScanUiState()
}

/**
 * ViewModel for the barcode scan screen.
 * Checks whether the product database has been populated, performs barcode lookups,
 * and exposes the current [ScanUiState].
 */
class ScanViewModel(
    private val repository: ProductRepository,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Loading)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        checkDatabasePopulated()
    }

    private fun checkDatabasePopulated() {
        viewModelScope.launch {
            _uiState.value = if (repository.hasProducts()) ScanUiState.Scanning else ScanUiState.NeedsSync
        }
    }

    /** Enqueues the Open Food Facts sync and tracks progress until it completes. */
    fun triggerProductSync() {
        workManager.enqueueUniqueWork(
            OFF_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<OpenFoodFactsWorker>().build(),
        )
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(OFF_SYNC_WORK_NAME).collect { infos ->
                val info = infos.firstOrNull() ?: return@collect
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        val status = info.progress.getString("status") ?: "Downloading…"
                        _uiState.value = ScanUiState.Syncing(status)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        checkDatabasePopulated()
                        return@collect
                    }
                    WorkInfo.State.FAILED -> {
                        _uiState.value = ScanUiState.NeedsSync
                        return@collect
                    }
                    else -> {}
                }
            }
        }
    }

    /** Called when the barcode analyzer detects a barcode. Ignored unless state is [ScanUiState.Scanning]. */
    fun onBarcodeDetected(barcode: String) {
        if (_uiState.value !is ScanUiState.Scanning) return
        viewModelScope.launch {
            val product = repository.getByBarcode(barcode)
            _uiState.value = if (product != null) {
                ScanUiState.Found(product.productJson)
            } else {
                ScanUiState.NotFound(barcode)
            }
        }
    }

    fun resetToScanning() {
        _uiState.value = ScanUiState.Scanning
    }
}
