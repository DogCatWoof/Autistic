package org.meow.autistic.ui.screens

import android.app.Application
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.meow.autistic.data.sequence.SequenceEntity
import org.meow.autistic.data.sequence.SequenceRepository
import org.meow.autistic.data.sequence.SequenceRunEntity
import org.meow.autistic.data.sequence.SequenceRunNotificationManager
import org.meow.autistic.data.sequence.SequenceStepEntity
import org.meow.autistic.data.sequence.SequenceStepProgressEntity
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SequenceRunViewModelTest {

    private val repository = mockk<SequenceRepository>(relaxed = true)
    private val application = mockk<Application>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: SequenceRunViewModel

    private val sequence = SequenceEntity(id = 1L, name = "Morning Routine")
    private val steps = listOf(
        SequenceStepEntity(id = 10L, sequenceId = 1L, instruction = "Step 1", position = 0),
        SequenceStepEntity(id = 11L, sequenceId = 1L, instruction = "Step 2", position = 1),
    )
    private val run = SequenceRunEntity(id = 5L, sequenceId = 1L, startedAt = Instant.EPOCH)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(SequenceRunNotificationManager)
        coEvery { SequenceRunNotificationManager.update(any(), any()) } returns Unit
        every { SequenceRunNotificationManager.cancel(any()) } returns Unit
        every { repository.getActiveRun() } returns flowOf(null)
        every { repository.getProgress(any()) } returns flowOf(emptyList())
        viewModel = SequenceRunViewModel(repository, application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(SequenceRunNotificationManager)
    }

    @Test
    fun `initial state is Idle when no active run`() = runTest {
        assertTrue(viewModel.state.first() is SequenceRunUiState.Idle)
    }

    @Test
    fun `state becomes Active when a run exists`() = runTest {
        every { repository.getActiveRun() } returns flowOf(run)
        every { repository.getProgress(run.id) } returns flowOf(emptyList())
        coEvery { repository.getById(sequence.id) } returns sequence
        coEvery { repository.getStepsOnce(sequence.id) } returns steps

        val vm = SequenceRunViewModel(repository, application)
        val state = vm.state.first()
        assertTrue(state is SequenceRunUiState.Active)
        val active = state as SequenceRunUiState.Active
        assertEquals(sequence, active.sequence)
        assertEquals(steps, active.steps)
        assertEquals(emptySet<Long>(), active.completedStepIds)
        assertEquals(steps.first(), active.currentStep)
        assertEquals(0f, active.progressFraction)
    }

    @Test
    fun `Active state reflects completed steps`() = runTest {
        val progress = listOf(SequenceStepProgressEntity(5L, 10L, Instant.EPOCH))
        every { repository.getActiveRun() } returns flowOf(run)
        every { repository.getProgress(run.id) } returns flowOf(progress)
        coEvery { repository.getById(sequence.id) } returns sequence
        coEvery { repository.getStepsOnce(sequence.id) } returns steps

        val vm = SequenceRunViewModel(repository, application)
        val active = vm.state.first() as SequenceRunUiState.Active
        assertEquals(setOf(10L), active.completedStepIds)
        assertEquals(steps[1], active.currentStep)
        assertEquals(0.5f, active.progressFraction)
    }

    @Test
    fun `startRun calls repository and updates notification`() = runTest {
        coEvery { repository.startRun(1L) } returns 5L
        viewModel.startRun(1L)
        coVerify { repository.startRun(1L) }
        coVerify { SequenceRunNotificationManager.update(application, 5L) }
    }

    @Test
    fun `completeStep marks step done and updates notification when run continues`() = runTest {
        coEvery { repository.getActiveRunOnce() } returns run
        coEvery { repository.getStepsOnce(sequence.id) } returns steps
        coEvery { repository.getProgressOnce(run.id) } returns listOf(
            SequenceStepProgressEntity(run.id, 10L, Instant.EPOCH)
        )
        viewModel.completeStep(run.id, 10L)
        coVerify { repository.completeStep(run.id, 10L) }
        coVerify { SequenceRunNotificationManager.update(application, run.id) }
    }

    @Test
    fun `completeStep finishes run when all steps are done`() = runTest {
        coEvery { repository.getActiveRunOnce() } returns run
        coEvery { repository.getStepsOnce(sequence.id) } returns steps
        coEvery { repository.getProgressOnce(run.id) } returns listOf(
            SequenceStepProgressEntity(run.id, 10L, Instant.EPOCH),
            SequenceStepProgressEntity(run.id, 11L, Instant.EPOCH),
        )
        viewModel.completeStep(run.id, 11L)
        coVerify { repository.completeRun(run.id) }
        coVerify { SequenceRunNotificationManager.cancel(application) }
    }

    @Test
    fun `endRun completes run and cancels notification`() = runTest {
        viewModel.endRun(run.id)
        coVerify { repository.completeRun(run.id) }
        coVerify { SequenceRunNotificationManager.cancel(application) }
    }
}
