package org.meow.autistic.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.meow.autistic.data.product.ProductRepository
sealed class ScanUiState {
    object Loading : ScanUiState()
    object NeedsSync : ScanUiState()
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
