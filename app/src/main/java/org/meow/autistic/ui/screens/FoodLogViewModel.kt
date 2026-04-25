package org.meow.autistic.ui.screens

import android.app.Application
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
import org.meow.autistic.data.debug.ExceptionReporter
import org.meow.autistic.data.foodlog.FoodCacheRepository
import org.meow.autistic.data.foodlog.FoodLogEntry
import org.meow.autistic.data.foodlog.FoodLogItemEntry
import org.meow.autistic.data.foodlog.FoodLogRepository
import org.meow.autistic.data.photo.ClaudeVisionClient
import org.meow.autistic.data.photo.FoodProductLookupService
import org.meow.autistic.data.photo.ParsedNutritionData
import org.meow.autistic.data.photo.PhotoType
import java.io.File
import java.time.Instant
import java.time.LocalDate

sealed class PhotoFlowState {
    object Idle : PhotoFlowState()
    data class Classifying(val itemId: Long) : PhotoFlowState()
    data class LookingUp(val itemId: Long) : PhotoFlowState()
    data class Ready(val data: ParsedNutritionData, val itemId: Long, val imagePath: String) : PhotoFlowState()
    data class NeedNutritionPhoto(val name: String?, val itemId: Long) : PhotoFlowState()
    data class OcrLoading(val itemId: Long, val imagePath: String) : PhotoFlowState()
    data class Error(val itemId: Long) : PhotoFlowState()
}

enum class PhotoAnalysisStatus { Analyzing, Done, Failed }

/**
 * ViewModel for the food log daily tracker.
 * Manages a unified photo analysis flow: classify → lookup or AI estimate → editable dialog.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FoodLogViewModel(
    private val repository: FoodLogRepository,
    private val claudeClient: ClaudeVisionClient,
    private val exceptionReporter: ExceptionReporter,
    private val foodCacheRepository: FoodCacheRepository,
    private val lookupService: FoodProductLookupService,
    application: Application,
) : AndroidViewModel(application) {

    private val handler = CoroutineExceptionHandler { _, e -> exceptionReporter.report(e) }

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

    private val _photoFlowState = MutableStateFlow<PhotoFlowState>(PhotoFlowState.Idle)
    val photoFlowState: StateFlow<PhotoFlowState> = _photoFlowState.asStateFlow()

    private val _photoAnalysisStatuses = MutableStateFlow<Map<Long, PhotoAnalysisStatus>>(emptyMap())
    val photoAnalysisStatuses: StateFlow<Map<Long, PhotoAnalysisStatus>> = _photoAnalysisStatuses.asStateFlow()

    private val _foodSuggestions = MutableStateFlow<List<ParsedNutritionData>>(emptyList())
    val foodSuggestions: StateFlow<List<ParsedNutritionData>> = _foodSuggestions.asStateFlow()

    init {
        viewModelScope.launch(handler) { clearStalePendingItems() }
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

    fun acceptPhoto() {
        viewModelScope.launch(handler) {
            val imagePath = savePhotoToStorage()
            val itemId = repository.insertItem(FoodLogItemEntry(
                date = _date.value,
                loggedAt = Instant.now(),
                imagePath = imagePath,
                description = "Analyzing photo…",
                isAiPending = true,
            ))
            _photoAnalysisStatuses.update { it + (itemId to PhotoAnalysisStatus.Analyzing) }
            _photoFlowState.value = PhotoFlowState.Classifying(itemId)
            classifyAndProcess(itemId, imagePath)
        }
    }

    private suspend fun classifyAndProcess(itemId: Long, imagePath: String) {
        try {
            val result = claudeClient.classifyAndAnalyze(imagePath)
            when (result.type) {
                PhotoType.FOOD -> {
                    val data = (result.nutrition ?: ParsedNutritionData())
                        .let { if (result.name != null) it.copy(description = result.name) else it }
                    _photoFlowState.value = PhotoFlowState.Ready(data, itemId, imagePath)
                }
                PhotoType.PRODUCT, PhotoType.UNKNOWN -> {
                    _photoFlowState.value = PhotoFlowState.LookingUp(itemId)
                    val found = lookupService.lookup(result.name, result.barcode)
                    if (found != null) {
                        _photoFlowState.value = PhotoFlowState.Ready(found, itemId, imagePath)
                    } else {
                        _photoFlowState.value = PhotoFlowState.NeedNutritionPhoto(result.name, itemId)
                    }
                }
            }
        } catch (e: Exception) {
            _photoAnalysisStatuses.update { it + (itemId to PhotoAnalysisStatus.Failed) }
            _photoFlowState.value = PhotoFlowState.Error(itemId)
            throw e
        }
    }

    fun continueWithNutritionPhoto(itemId: Long, name: String?) {
        viewModelScope.launch(handler) {
            val imagePath = savePhotoToStorage()
            _photoFlowState.value = PhotoFlowState.OcrLoading(itemId, imagePath)
            try {
                val data = claudeClient.analyzeNutritionLabel(imagePath)
                val namedData = if (!name.isNullOrBlank()) data.copy(description = name) else data
                _photoFlowState.value = PhotoFlowState.Ready(namedData, itemId, imagePath)
            } catch (e: Exception) {
                _photoAnalysisStatuses.update { it + (itemId to PhotoAnalysisStatus.Failed) }
                _photoFlowState.value = PhotoFlowState.Error(itemId)
                throw e
            }
        }
    }

    fun savePhotoEntry(data: ParsedNutritionData, servings: Double, itemId: Long, imagePath: String) {
        val s = servings.coerceAtLeast(0.0)
        _photoAnalysisStatuses.update { it + (itemId to PhotoAnalysisStatus.Done) }
        _photoFlowState.value = PhotoFlowState.Idle
        viewModelScope.launch(handler) {
            val item = repository.getItemById(itemId)
                ?: throw IllegalStateException("Food log item $itemId not found after photo analysis")
            foodCacheRepository.save(data)
            repository.updateItem(item.copy(
                description = data.description,
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

    fun dismissPhotoFlow() {
        val state = _photoFlowState.value
        _photoFlowState.value = PhotoFlowState.Idle
        val itemId = when (state) {
            is PhotoFlowState.Classifying -> state.itemId
            is PhotoFlowState.LookingUp -> state.itemId
            is PhotoFlowState.Ready -> state.itemId
            is PhotoFlowState.NeedNutritionPhoto -> state.itemId
            is PhotoFlowState.OcrLoading -> state.itemId
            is PhotoFlowState.Error -> state.itemId
            PhotoFlowState.Idle -> return
        }
        _photoAnalysisStatuses.update { it - itemId }
        viewModelScope.launch(handler) {
            val item = repository.getItemById(itemId) ?: return@launch
            if (item.calories == 0.0) { repository.deleteItem(item) }
        }
    }

    fun searchFoodNames(query: String) {
        if (query.isBlank()) { _foodSuggestions.value = emptyList(); return }
        viewModelScope.launch(handler) {
            _foodSuggestions.value = foodCacheRepository.search(query)
        }
    }

    private fun savePhotoToStorage(): String {
        val app = getApplication<Application>()
        val destDir = File(app.filesDir, "food_log_images").also { it.mkdirs() }
        val destFile = File(destDir, "${System.currentTimeMillis()}.jpg")
        File(app.cacheDir, "camera_temp/temp_food_photo.jpg").copyTo(destFile, overwrite = true)
        return destFile.absolutePath
    }

    private suspend fun clearStalePendingItems() {
        repository.getPendingAnalysisItems().forEach { item ->
            repository.updateItem(item.copy(isAiPending = false, description = null))
        }
    }
}
