package org.meow.autistic.ui.screens

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.meow.autistic.data.debug.ExceptionReporter
import org.meow.autistic.data.foodlog.FoodCacheRepository
import org.meow.autistic.data.foodlog.FoodLogEntry
import org.meow.autistic.data.foodlog.FoodLogItemEntry
import org.meow.autistic.data.foodlog.FoodLogRepository
import org.meow.autistic.data.photo.ClaudeVisionClient
import org.meow.autistic.data.photo.ParsedNutritionData
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.resume

sealed class LabelFlowState {
    object Idle : LabelFlowState()
    object ExtractingName : LabelFlowState()
    data class NeedNutritionPhoto(val name: String, val itemId: Long, val labelImagePath: String) : LabelFlowState()
}

sealed class LabelAnalysisState {
    object Idle : LabelAnalysisState()
    data class Loading(val itemId: Long, val imagePath: String) : LabelAnalysisState()
    data class Ready(val data: ParsedNutritionData, val itemId: Long, val imagePath: String) : LabelAnalysisState()
    data class Error(val itemId: Long, val imagePath: String) : LabelAnalysisState()
}

enum class PhotoAnalysisStatus { ScanningLabel, Queued, Analyzing, Done, Failed }

sealed class FoodPhotoAnalysisState {
    object Idle : FoodPhotoAnalysisState()
    data class Loading(val itemId: Long) : FoodPhotoAnalysisState()
    data class Ready(val data: ParsedNutritionData, val itemId: Long, val imagePath: String) : FoodPhotoAnalysisState()
    data class Error(val itemId: Long) : FoodPhotoAnalysisState()
}

/**
 * ViewModel for the food log daily tracker.
 * Individual [FoodLogItemEntry] records are summed to produce daily nutrition totals.
 * Manages Claude-powered nutrition label OCR (Path 1) and WiFi-only food photo analysis (Path 2).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FoodLogViewModel(
    private val repository: FoodLogRepository,
    private val claudeClient: ClaudeVisionClient,
    private val exceptionReporter: ExceptionReporter,
    private val foodCacheRepository: FoodCacheRepository,
    application: Application,
) : AndroidViewModel(application) {

    private val handler = CoroutineExceptionHandler { _, e -> exceptionReporter.report(e) }
    private val connectivityManager = application.getSystemService(ConnectivityManager::class.java)

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FoodLogEntry(date = LocalDate.now().toString()))

    private val _labelFlowState = MutableStateFlow<LabelFlowState>(LabelFlowState.Idle)
    val labelFlowState: StateFlow<LabelFlowState> = _labelFlowState.asStateFlow()

    private val _foodSuggestions = MutableStateFlow<List<ParsedNutritionData>>(emptyList())
    val foodSuggestions: StateFlow<List<ParsedNutritionData>> = _foodSuggestions.asStateFlow()

    private val _labelAnalysisState = MutableStateFlow<LabelAnalysisState>(LabelAnalysisState.Idle)
    val labelAnalysisState: StateFlow<LabelAnalysisState> = _labelAnalysisState.asStateFlow()

    private val _foodPhotoAnalysisState = MutableStateFlow<FoodPhotoAnalysisState>(FoodPhotoAnalysisState.Idle)
    val foodPhotoAnalysisState: StateFlow<FoodPhotoAnalysisState> = _foodPhotoAnalysisState.asStateFlow()

    private val _photoAnalysisStatuses = MutableStateFlow<Map<Long, PhotoAnalysisStatus>>(emptyMap())
    val photoAnalysisStatuses: StateFlow<Map<Long, PhotoAnalysisStatus>> = _photoAnalysisStatuses.asStateFlow()

    init {
        viewModelScope.launch(handler) { requeuePendingItems() }
    }

    fun previousDay() { _date.value = LocalDate.parse(_date.value).minusDays(1).toString() }
    fun nextDay() { _date.value = LocalDate.parse(_date.value).plusDays(1).toString() }

    fun addItem(item: FoodLogItemEntry) {
        viewModelScope.launch(handler) {
            repository.insertItem(item.copy(id = 0, date = _date.value, loggedAt = Instant.now()))
        }
    }

    fun deleteItem(item: FoodLogItemEntry) {
        viewModelScope.launch(handler) { repository.deleteItem(item) }
    }

    // ── Path 1: Nutrition label ─────────────────────────────────────────────

    fun startLabelAnalysis(imagePath: String) {
        viewModelScope.launch(handler) {
            val itemId = repository.insertItem(FoodLogItemEntry(
                date = _date.value,
                loggedAt = Instant.now(),
                imagePath = imagePath,
                description = "Scanning nutrition label…",
                isAiPending = true,
            ))
            _photoAnalysisStatuses.update { it + (itemId to PhotoAnalysisStatus.ScanningLabel) }
            _labelAnalysisState.value = LabelAnalysisState.Loading(itemId, imagePath)
            try {
                val data = claudeClient.analyzeNutritionLabel(imagePath)
                _labelAnalysisState.value = LabelAnalysisState.Ready(data, itemId, imagePath)
            } catch (e: Exception) {
                _labelAnalysisState.value = LabelAnalysisState.Error(itemId, imagePath)
                throw e
            }
        }
    }

    fun dismissLabelAnalysis() {
        val state = _labelAnalysisState.value
        _labelAnalysisState.value = LabelAnalysisState.Idle
        val itemId = when (state) {
            is LabelAnalysisState.Loading -> state.itemId
            is LabelAnalysisState.Ready -> state.itemId
            is LabelAnalysisState.Error -> state.itemId
            else -> return
        }
        _photoAnalysisStatuses.update { it - itemId }
        viewModelScope.launch(handler) {
            val item = repository.getItemById(itemId) ?: return@launch  // already deleted, nothing to do
            if (item.calories == 0.0) repository.deleteItem(item)
        }
    }

    fun saveLabelEntry(data: ParsedNutritionData, servings: Double, itemId: Long, imagePath: String) {
        val s = servings.coerceAtLeast(0.0)
        _photoAnalysisStatuses.update { it - itemId }
        _labelAnalysisState.value = LabelAnalysisState.Idle
        viewModelScope.launch(handler) {
            val existing = repository.getItemById(itemId)
                ?: throw IllegalStateException("Food log item $itemId not found after label scan")
            repository.updateItem(existing.copy(
                description = null,
                imagePath = imagePath,
                isAiPending = false,
                calories = data.calories * s,
                protein = data.protein * s,
                totalFat = data.totalFat * s,
                totalCarbs = data.totalCarbs * s,
                fiber = data.fiber * s,
                totalSugars = data.totalSugars * s,
                addedSugars = data.addedSugars * s,
                sugarAlcohols = data.sugarAlcohols * s,
            ))
        }
    }

    // ── Path 2: Food photo AI analysis ─────────────────────────────────────

    fun acceptLabelPhoto() {
        viewModelScope.launch(handler) {
            startLabelFlow(savePhotoToStorage())
        }
    }

    private suspend fun startLabelFlow(imagePath: String) {
        val itemId = repository.insertItem(FoodLogItemEntry(
            date = _date.value,
            loggedAt = Instant.now(),
            imagePath = imagePath,
            description = "Extracting food name…",
            isAiPending = true,
        ))
        _photoAnalysisStatuses.update { it + (itemId to PhotoAnalysisStatus.ScanningLabel) }
        _labelFlowState.value = LabelFlowState.ExtractingName
        try {
            val name = claudeClient.extractFoodName(imagePath)
            if (name != null) {
                val cached = foodCacheRepository.findByDescription(name)
                if (cached != null) {
                    _labelFlowState.value = LabelFlowState.Idle
                    _labelAnalysisState.value = LabelAnalysisState.Ready(cached, itemId, imagePath)
                    return
                }
            }
            _labelFlowState.value = LabelFlowState.NeedNutritionPhoto(name ?: "", itemId, imagePath)
        } catch (e: Exception) {
            _labelFlowState.value = LabelFlowState.Idle
            _labelAnalysisState.value = LabelAnalysisState.Error(itemId, imagePath)
            throw e
        }
    }

    fun continueLabelWithNutritionPhoto(itemId: Long, foodName: String) {
        viewModelScope.launch(handler) {
            val nutritionImagePath = savePhotoToStorage()
            _labelFlowState.value = LabelFlowState.Idle
            _photoAnalysisStatuses.update { it + (itemId to PhotoAnalysisStatus.ScanningLabel) }
            _labelAnalysisState.value = LabelAnalysisState.Loading(itemId, nutritionImagePath)
            try {
                val data = claudeClient.analyzeNutritionLabel(nutritionImagePath)
                val namedData = if (foodName.isNotBlank()) data.copy(description = foodName) else data
                _labelAnalysisState.value = LabelAnalysisState.Ready(namedData, itemId, nutritionImagePath)
            } catch (e: Exception) {
                _labelAnalysisState.value = LabelAnalysisState.Error(itemId, nutritionImagePath)
                throw e
            }
        }
    }

    fun dismissLabelFlow() {
        val state = _labelFlowState.value
        _labelFlowState.value = LabelFlowState.Idle
        if (state is LabelFlowState.NeedNutritionPhoto) {
            _photoAnalysisStatuses.update { it - state.itemId }
            viewModelScope.launch(handler) {
                val item = repository.getItemById(state.itemId) ?: return@launch
                if (item.calories == 0.0) { repository.deleteItem(item) }
            }
        }
    }

    fun searchFoodNames(query: String) {
        if (query.isBlank()) { _foodSuggestions.value = emptyList(); return }
        viewModelScope.launch(handler) {
            _foodSuggestions.value = foodCacheRepository.search(query)
        }
    }

    fun acceptFoodPhoto() {
        viewModelScope.launch(handler) {
            queueFoodPhotoItem(savePhotoToStorage())
        }
    }

    private fun savePhotoToStorage(): String {
        val app = getApplication<Application>()
        val destDir = File(app.filesDir, "food_log_images").also { it.mkdirs() }
        val destFile = File(destDir, "${System.currentTimeMillis()}.jpg")
        File(app.cacheDir, "camera_temp/temp_food_photo.jpg").copyTo(destFile, overwrite = true)
        return destFile.absolutePath
    }

    fun queueFoodPhotoItem(imagePath: String) {
        viewModelScope.launch(handler) {
            val id = repository.insertItem(FoodLogItemEntry(
                date = _date.value,
                loggedAt = Instant.now(),
                imagePath = imagePath,
                isAiPending = true,
            ))
            launchPhotoAnalysis(id, imagePath)
        }
    }

    private fun launchPhotoAnalysis(itemId: Long, imagePath: String) {
        _photoAnalysisStatuses.update { it + (itemId to PhotoAnalysisStatus.Queued) }
        viewModelScope.launch(handler) { processPhotoAnalysis(itemId, imagePath) }
    }

    private suspend fun processPhotoAnalysis(itemId: Long, imagePath: String) {
        _foodPhotoAnalysisState.value = FoodPhotoAnalysisState.Loading(itemId)
        while (true) {
            if (!isWifiAvailable()) {
                _photoAnalysisStatuses.update { it + (itemId to PhotoAnalysisStatus.Queued) }
                awaitWifi()
            }
            _photoAnalysisStatuses.update { it + (itemId to PhotoAnalysisStatus.Analyzing) }
            try {
                val result = claudeClient.analyzeFoodPhoto(imagePath)
                val data = result.description?.let { foodCacheRepository.findByDescription(it) } ?: result
                _photoAnalysisStatuses.update { it + (itemId to PhotoAnalysisStatus.Done) }
                _foodPhotoAnalysisState.value = FoodPhotoAnalysisState.Ready(data, itemId, imagePath)
                return
            } catch (e: IOException) {
                _photoAnalysisStatuses.update { it + (itemId to PhotoAnalysisStatus.Queued) }
                awaitWifi()
            } catch (e: Exception) {
                val item = repository.getItemById(itemId)
                if (item != null) {
                    repository.updateItem(item.copy(isAiPending = false))
                }
                _photoAnalysisStatuses.update { it + (itemId to PhotoAnalysisStatus.Failed) }
                _foodPhotoAnalysisState.value = FoodPhotoAnalysisState.Error(itemId)
                throw e
            }
        }
    }

    fun saveFoodPhotoEntry(data: ParsedNutritionData, servings: Double, itemId: Long) {
        viewModelScope.launch(handler) {
            val s = servings.coerceAtLeast(0.0)
            val item = repository.getItemById(itemId) ?: return@launch
            _foodPhotoAnalysisState.value = FoodPhotoAnalysisState.Idle
            _photoAnalysisStatuses.update { it - itemId }
            foodCacheRepository.save(data)
            repository.updateItem(item.copy(
                description = data.description,
                isAiPending = false,
                calories = data.calories * s,
                protein = data.protein * s,
                totalFat = data.totalFat * s,
                totalCarbs = data.totalCarbs * s,
                fiber = data.fiber * s,
                totalSugars = data.totalSugars * s,
                addedSugars = data.addedSugars * s,
                sugarAlcohols = data.sugarAlcohols * s,
            ))
        }
    }

    fun dismissFoodPhotoAnalysis() {
        val state = _foodPhotoAnalysisState.value
        _foodPhotoAnalysisState.value = FoodPhotoAnalysisState.Idle
        val itemId = when (state) {
            is FoodPhotoAnalysisState.Loading -> state.itemId
            is FoodPhotoAnalysisState.Ready -> state.itemId
            is FoodPhotoAnalysisState.Error -> state.itemId
            else -> return
        }
        _photoAnalysisStatuses.update { it - itemId }
    }

    private suspend fun requeuePendingItems() {
        repository.getPendingAnalysisItems().forEach { item ->
            launchPhotoAnalysis(item.id, item.imagePath ?: return@forEach)
        }
    }

    private fun isWifiAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private suspend fun awaitWifi() = suspendCancellableCoroutine<Unit> { cont ->
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val caps = connectivityManager.getNetworkCapabilities(network) ?: return
                if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return
                connectivityManager.unregisterNetworkCallback(this)
                if (cont.isActive) cont.resume(Unit)
            }
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        cont.invokeOnCancellation {
            try { connectivityManager.unregisterNetworkCallback(callback) } catch (_: IllegalArgumentException) {}
        }
    }
}
