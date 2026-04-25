package org.meow.autistic.ui.screens

import android.app.Application
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.meow.autistic.data.debug.ExceptionReporter
import org.meow.autistic.data.foodlog.FoodCacheRepository
import org.meow.autistic.data.foodlog.FoodLogItemEntry
import org.meow.autistic.data.foodlog.FoodLogRepository
import org.meow.autistic.data.photo.ClaudeVisionClient
import org.meow.autistic.data.photo.FoodProductLookupService
import org.meow.autistic.data.photo.ParsedNutritionData
import org.meow.autistic.data.photo.PhotoClassificationResult
import org.meow.autistic.data.photo.PhotoType
import java.io.File
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FoodLogViewModelTest {

    private val repository = mockk<FoodLogRepository>(relaxed = true)
    private val claudeClient = mockk<ClaudeVisionClient>(relaxed = true)
    private val exceptionReporter = mockk<ExceptionReporter>(relaxed = true)
    private val foodCacheRepository = mockk<FoodCacheRepository>(relaxed = true)
    private val lookupService = mockk<FoodProductLookupService>(relaxed = true)
    private val application = mockk<Application>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val tempDir = File(System.getProperty("java.io.tmpdir"), "foodlog_test_${System.nanoTime()}")
    private lateinit var viewModel: FoodLogViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tempDir.mkdirs()
        val cacheDir = File(tempDir, "cache")
        File(cacheDir, "camera_temp").mkdirs()
        File(cacheDir, "camera_temp/temp_food_photo.jpg").apply { parentFile?.mkdirs(); createNewFile() }
        every { application.filesDir } returns tempDir
        every { application.cacheDir } returns cacheDir
        coEvery { repository.getPendingAnalysisItems() } returns emptyList()
        coEvery { repository.insertItem(any()) } returns 1L
        viewModel = FoodLogViewModel(repository, claudeClient, exceptionReporter, foodCacheRepository, lookupService, application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    // region initial state

    @Test
    fun `initial photoFlowState is Idle`() {
        assertEquals(PhotoFlowState.Idle, viewModel.photoFlowState.value)
    }

    @Test
    fun `initial photoAnalysisStatuses is empty`() {
        assertTrue(viewModel.photoAnalysisStatuses.value.isEmpty())
    }

    // endregion

    // region acceptPhoto classification paths

    @Test
    fun `acceptPhoto with food result transitions to Ready with nutrition data`() = runTest {
        val nutrition = ParsedNutritionData(description = "Salad", calories = 200.0)
        coEvery { claudeClient.classifyAndAnalyze(any()) } returns PhotoClassificationResult(
            type = PhotoType.FOOD, name = "Salad", barcode = null, nutrition = nutrition,
        )
        viewModel.acceptPhoto()
        val state = viewModel.photoFlowState.value
        assertTrue(state is PhotoFlowState.Ready)
        assertEquals("Salad", (state as PhotoFlowState.Ready).data.description)
        assertEquals(200.0, state.data.calories, 0.001)
    }

    @Test
    fun `acceptPhoto with product and cache hit transitions to Ready`() = runTest {
        val cached = ParsedNutritionData(description = "Chips", calories = 150.0)
        coEvery { claudeClient.classifyAndAnalyze(any()) } returns PhotoClassificationResult(
            type = PhotoType.PRODUCT, name = "Chips", barcode = null, nutrition = null,
        )
        coEvery { lookupService.lookup("Chips", null) } returns cached
        viewModel.acceptPhoto()
        val state = viewModel.photoFlowState.value
        assertTrue(state is PhotoFlowState.Ready)
        assertEquals(150.0, (state as PhotoFlowState.Ready).data.calories, 0.001)
    }

    @Test
    fun `acceptPhoto with product and no lookup result transitions to NeedNutritionPhoto`() = runTest {
        coEvery { claudeClient.classifyAndAnalyze(any()) } returns PhotoClassificationResult(
            type = PhotoType.PRODUCT, name = "Unknown Brand", barcode = "012345", nutrition = null,
        )
        coEvery { lookupService.lookup(any(), any()) } returns null
        viewModel.acceptPhoto()
        val state = viewModel.photoFlowState.value
        assertTrue(state is PhotoFlowState.NeedNutritionPhoto)
        assertEquals("Unknown Brand", (state as PhotoFlowState.NeedNutritionPhoto).name)
    }

    @Test
    fun `acceptPhoto with unknown type and no lookup transitions to NeedNutritionPhoto`() = runTest {
        coEvery { claudeClient.classifyAndAnalyze(any()) } returns PhotoClassificationResult(
            type = PhotoType.UNKNOWN, name = null, barcode = null, nutrition = null,
        )
        coEvery { lookupService.lookup(any(), any()) } returns null
        viewModel.acceptPhoto()
        assertTrue(viewModel.photoFlowState.value is PhotoFlowState.NeedNutritionPhoto)
    }

    @Test
    fun `acceptPhoto sets Analyzing status on item`() = runTest {
        coEvery { claudeClient.classifyAndAnalyze(any()) } returns PhotoClassificationResult(
            type = PhotoType.FOOD, name = "Apple", barcode = null,
            nutrition = ParsedNutritionData(description = "Apple", calories = 95.0),
        )
        viewModel.acceptPhoto()
        assertTrue(viewModel.photoAnalysisStatuses.value.isNotEmpty())
    }

    // endregion

    // region dismissPhotoFlow

    @Test
    fun `dismissPhotoFlow from Idle is a no-op`() {
        viewModel.dismissPhotoFlow()
        assertEquals(PhotoFlowState.Idle, viewModel.photoFlowState.value)
    }

    // endregion

    // region savePhotoEntry

    @Test
    fun `savePhotoEntry transitions to Idle and sets Done status`() = runTest {
        val itemId = 42L
        val item = FoodLogItemEntry(id = itemId, date = "2025-01-01", loggedAt = Instant.now(), calories = 0.0)
        coEvery { repository.getItemById(itemId) } returns item
        val data = ParsedNutritionData(description = "Apple", calories = 95.0)
        viewModel.savePhotoEntry(data, 1.0, itemId, "/some/path.jpg")
        assertEquals(PhotoFlowState.Idle, viewModel.photoFlowState.value)
        assertEquals(PhotoAnalysisStatus.Done, viewModel.photoAnalysisStatuses.value[itemId])
    }

    @Test
    fun `savePhotoEntry saves nutrition data to cache`() = runTest {
        val itemId = 5L
        val item = FoodLogItemEntry(id = itemId, date = "2025-01-01", loggedAt = Instant.now())
        coEvery { repository.getItemById(itemId) } returns item
        val data = ParsedNutritionData(description = "Banana", calories = 89.0)
        viewModel.savePhotoEntry(data, 1.0, itemId, "/path.jpg")
        coVerify { foodCacheRepository.save(data) }
    }

    @Test
    fun `savePhotoEntry multiplies nutrients by servings`() = runTest {
        val itemId = 3L
        val item = FoodLogItemEntry(id = itemId, date = "2025-01-01", loggedAt = Instant.now())
        coEvery { repository.getItemById(itemId) } returns item
        val data = ParsedNutritionData(description = "Rice", calories = 200.0, protein = 4.0)
        viewModel.savePhotoEntry(data, 2.5, itemId, "/path.jpg")
        coVerify { repository.updateItem(match { it.calories == 500.0 && it.protein == 10.0 }) }
    }

    // endregion
}
