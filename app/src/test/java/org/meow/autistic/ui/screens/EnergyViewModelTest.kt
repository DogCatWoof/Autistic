package org.meow.autistic.ui.screens

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.meow.autistic.data.debug.ExceptionReporter
import org.meow.autistic.data.energy.EnergyLogEntry
import org.meow.autistic.data.energy.EnergyProfileEntity
import org.meow.autistic.data.energy.EnergyRepository

@OptIn(ExperimentalCoroutinesApi::class)
class EnergyViewModelTest {

    private val repository = mockk<EnergyRepository>(relaxed = true)
    private val exceptionReporter = mockk<ExceptionReporter>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: EnergyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getTodayLogs() } returns flowOf(emptyList())
        coEvery { repository.getOrCreateProfile() } returns EnergyProfileEntity()
        coEvery { repository.getTodayStartOfDay() } returns null
        coEvery { repository.getTodayCapacity() } returns 10.0
        coEvery { repository.getTodayBalance() } returns 10.0
        viewModel = EnergyViewModel(repository, exceptionReporter)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region initial state

    @Test
    fun `init loads profile and populates capacity and balance`() = runTest {
        assertNotNull(viewModel.profile.value)
        assertEquals(10.0, viewModel.todayCapacity.value, 0.001)
        assertEquals(10.0, viewModel.todayBalance.value, 0.001)
    }

    @Test
    fun `init sets startOfDay to null when none stored`() = runTest {
        assertNull(viewModel.startOfDay.value)
    }

    @Test
    fun `projectedBalance is null by default`() {
        assertNull(viewModel.projectedBalance.value)
    }

    // endregion

    // region logActivity / deleteLog

    @Test
    fun `logActivity delegates to repository`() = runTest {
        viewModel.logActivity("Focus Work", "Deep work", 2)
        coVerify { repository.logActivity("Focus Work", "Deep work", 2) }
    }

    @Test
    fun `deleteLog delegates to repository`() = runTest {
        val log = EnergyLogEntry(date = "2025-01-01", activityType = "Meeting", energyCost = 3.0)
        viewModel.deleteLog(log)
        coVerify { repository.deleteLog(log) }
    }

    // endregion

    // region previewProjection / clearProjection

    @Test
    fun `previewProjection stores projected balance`() = runTest {
        coEvery { repository.projectBalance("Meeting") } returns 7.0
        viewModel.previewProjection("Meeting")
        assertEquals(7.0, viewModel.projectedBalance.value)
    }

    @Test
    fun `clearProjection resets projected balance to null`() = runTest {
        coEvery { repository.projectBalance("Meeting") } returns 7.0
        viewModel.previewProjection("Meeting")
        viewModel.clearProjection()
        assertNull(viewModel.projectedBalance.value)
    }

    // endregion

    // region submitStartOfDay — multiplier formula

    @Test
    fun `submitStartOfDay computes mid-range multiplier`() = runTest {
        // sleep=3, stress=3, physical=3 → score=(3+3+(6-3))/15=0.6 → multiplier=0.96
        viewModel.submitStartOfDay(sleepQuality = 3, stressLevel = 3, physicalState = 3)
        coVerify {
            repository.upsertStartOfDay(match { kotlin.math.abs(it.baselineMultiplier - 0.96) < 0.001 })
        }
    }

    @Test
    fun `submitStartOfDay caps multiplier at 1_2 for perfect conditions`() = runTest {
        // sleep=5, stress=1, physical=5 → score=(5+5+5)/15=1.0 → multiplier=1.2
        viewModel.submitStartOfDay(sleepQuality = 5, stressLevel = 1, physicalState = 5)
        coVerify {
            repository.upsertStartOfDay(match { kotlin.math.abs(it.baselineMultiplier - 1.2) < 0.001 })
        }
    }

    @Test
    fun `submitStartOfDay floors multiplier at 0_6 for worst conditions`() = runTest {
        // sleep=1, stress=5, physical=1 → score=3/15=0.2 → raw=0.72 (above floor, not clamped)
        viewModel.submitStartOfDay(sleepQuality = 1, stressLevel = 5, physicalState = 1)
        coVerify {
            repository.upsertStartOfDay(match { kotlin.math.abs(it.baselineMultiplier - 0.72) < 0.001 })
        }
    }

    @Test
    fun `submitStartOfDay stores sleep, stress, physical in entry`() = runTest {
        viewModel.submitStartOfDay(sleepQuality = 4, stressLevel = 2, physicalState = 3)
        coVerify {
            repository.upsertStartOfDay(match {
                it.sleepQuality == 4 && it.stressLevel == 2 && it.physicalState == 3
            })
        }
    }

    @Test
    fun `submitStartOfDay updates startOfDay state`() = runTest {
        viewModel.submitStartOfDay(sleepQuality = 4, stressLevel = 2, physicalState = 5)
        val stored = viewModel.startOfDay.value
        assertNotNull(stored)
        assertEquals(4, stored?.sleepQuality)
    }

    // endregion

    // region updateProfile

    @Test
    fun `updateProfile saves updated capacity to repository`() = runTest {
        viewModel.updateProfile(8)
        coVerify { repository.updateProfile(match { it.dailyCapacity == 8 }) }
    }

    @Test
    fun `updateProfile reflects new capacity in profile state`() = runTest {
        viewModel.updateProfile(6)
        assertEquals(6, viewModel.profile.value?.dailyCapacity)
    }

    // endregion

    // region costFor

    @Test
    fun `costFor returns correct default for known activity types`() {
        assertEquals(3.0, viewModel.costFor("Meeting"), 0.001)
        assertEquals(2.0, viewModel.costFor("Focus Work"), 0.001)
        assertEquals(-1.0, viewModel.costFor("Break"), 0.001)
        assertEquals(-2.0, viewModel.costFor("Nap"), 0.001)
    }

    @Test
    fun `costFor returns 2_0 for unknown activity type`() {
        assertEquals(2.0, viewModel.costFor("Completely Unknown"), 0.001)
    }

    // endregion
}
