package org.meow.autistic.ui.screens

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.meow.autistic.data.product.ProductEntity
import org.meow.autistic.data.product.ProductRepository
import java.io.IOException
import kotlin.coroutines.resume

data class ScanQueueItem(
    val barcode: String,
    val status: ScanStatus,
)

sealed class ScanStatus {
    object Pending : ScanStatus()
    object Loading : ScanStatus()
    data class Found(val product: ProductEntity) : ScanStatus()
    object NotFound : ScanStatus()
    object NetworkError : ScanStatus()
}

/**
 * ViewModel for the barcode scan screen.
 * Maintains a queue of scanned barcodes, fetches product data for each,
 * and retries automatically when network connectivity is restored.
 */
class ScanViewModel(
    private val repository: ProductRepository,
    application: Application,
) : AndroidViewModel(application) {

    private val connectivityManager =
        application.getSystemService(ConnectivityManager::class.java)

    private val _queue = MutableStateFlow<List<ScanQueueItem>>(emptyList())
    val queue: StateFlow<List<ScanQueueItem>> = _queue.asStateFlow()

    private val _selectedBarcode = MutableStateFlow<String?>(null)
    val selectedBarcode: StateFlow<String?> = _selectedBarcode.asStateFlow()

    /**
     * Adds [barcode] to the queue and starts a lookup.
     * Ignored if the barcode is not a valid EAN/UPC format or is already queued.
     */
    fun onBarcodeDetected(barcode: String) {
        if (!isValidBarcode(barcode)) return
        if (_queue.value.any { it.barcode == barcode }) return
        _queue.update { it + ScanQueueItem(barcode, ScanStatus.Pending) }
        viewModelScope.launch { fetchProduct(barcode) }
    }

    /** Returns true if [barcode] is a numeric EAN-8, EAN-13, UPC-A, or similar product barcode. */
    private fun isValidBarcode(barcode: String): Boolean =
        barcode.length in 6..14 && barcode.all { it.isDigit() }

    /** Removes an item from the queue regardless of its current status. */
    fun dismiss(barcode: String) {
        _queue.update { it.filter { item -> item.barcode != barcode } }
        if (_selectedBarcode.value == barcode) _selectedBarcode.value = null
    }

    /** Toggles selection. Selecting an already-selected item deselects it. */
    fun select(barcode: String) {
        _selectedBarcode.value = if (_selectedBarcode.value == barcode) null else barcode
    }

    private suspend fun fetchProduct(barcode: String) {
        val cached = repository.getLocalByBarcode(barcode)
        if (cached != null) {
            updateStatus(barcode, ScanStatus.Found(cached))
            return
        }
        while (_queue.value.any { it.barcode == barcode }) {
            updateStatus(barcode, ScanStatus.Loading)
            try {
                val product = repository.getByBarcode(barcode)
                if (_queue.value.none { it.barcode == barcode }) return
                updateStatus(barcode, if (product != null) ScanStatus.Found(product) else ScanStatus.NotFound)
                return
            } catch (e: IOException) {
                if (_queue.value.none { it.barcode == barcode }) return
                updateStatus(barcode, ScanStatus.NetworkError)
                awaitNetwork()
            }
        }
    }

    private fun updateStatus(barcode: String, status: ScanStatus) {
        _queue.update { queue ->
            queue.map { if (it.barcode == barcode) it.copy(status = status) else it }
        }
    }

    /** Suspends until any network with internet capability becomes available. */
    private suspend fun awaitNetwork() = suspendCancellableCoroutine<Unit> { cont ->
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectivityManager.unregisterNetworkCallback(this)
                if (cont.isActive) cont.resume(Unit)
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        cont.invokeOnCancellation {
            try { connectivityManager.unregisterNetworkCallback(callback) } catch (_: IllegalArgumentException) {}
        }
    }
}
