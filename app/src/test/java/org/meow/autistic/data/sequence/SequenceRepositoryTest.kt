package org.meow.autistic.data.sequence

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.meow.autistic.data.diagnostics.QueryLogger
import java.time.Instant

class SequenceRepositoryTest {

    private lateinit var dao: SequenceDao
    private lateinit var repository: SequenceRepository

    private val sequence = SequenceEntity(id = 1L, name = "Morning Routine")
    private val step = SequenceStepEntity(id = 10L, sequenceId = 1L, instruction = "Brush teeth", position = 0)

    @Before
    fun setUp() {
        dao = mockk()
        every { dao.getAll() } returns flowOf(emptyList())
        every { dao.getSteps(any()) } returns flowOf(emptyList())
        every { dao.getActiveRun() } returns flowOf(null)
        every { dao.getProgress(any()) } returns flowOf(emptyList())
        repository = SequenceRepository(dao, QueryLogger())
    }

    @Test
    fun `getAllSequences exposes flow from dao`() = runTest {
        every { dao.getAll() } returns flowOf(listOf(sequence))
        val result = SequenceRepository(dao, QueryLogger()).getAllSequences().first()
        assertEquals(listOf(sequence), result)
    }

    @Test
    fun `insertSequence delegates to dao`() = runTest {
        coEvery { dao.insertSequence(sequence) } returns 1L
        repository.insertSequence(sequence)
        coVerify(exactly = 1) { dao.insertSequence(sequence) }
    }

    @Test
    fun `deleteSequence deletes steps then sequence`() = runTest {
        coEvery { dao.deleteStepsForSequence(sequence.id) } returns Unit
        coEvery { dao.deleteSequence(sequence) } returns Unit
        repository.deleteSequence(sequence)
        coVerify(exactly = 1) { dao.deleteStepsForSequence(sequence.id) }
        coVerify(exactly = 1) { dao.deleteSequence(sequence) }
    }

    @Test
    fun `insertStep delegates to dao`() = runTest {
        coEvery { dao.insertStep(step) } returns 10L
        repository.insertStep(step)
        coVerify(exactly = 1) { dao.insertStep(step) }
    }

    @Test
    fun `deleteStep delegates to dao`() = runTest {
        coEvery { dao.deleteStep(step) } returns Unit
        repository.deleteStep(step)
        coVerify(exactly = 1) { dao.deleteStep(step) }
    }

    @Test
    fun `startRun inserts a run and returns its id`() = runTest {
        coEvery { dao.insertRun(any()) } returns 5L
        val id = repository.startRun(sequence.id)
        assertEquals(5L, id)
        coVerify(exactly = 1) { dao.insertRun(match { it.sequenceId == sequence.id && it.completedAt == null }) }
    }

    @Test
    fun `completeRun updates run with completedAt timestamp`() = runTest {
        val run = SequenceRunEntity(id = 5L, sequenceId = 1L, startedAt = Instant.EPOCH)
        coEvery { dao.getRunById(5L) } returns run
        coEvery { dao.updateRun(any()) } returns Unit
        repository.completeRun(5L)
        coVerify { dao.updateRun(match { it.id == 5L && it.completedAt != null }) }
    }

    @Test(expected = IllegalStateException::class)
    fun `completeRun throws when run not found`() = runTest {
        coEvery { dao.getRunById(99L) } returns null
        repository.completeRun(99L)
    }

    @Test
    fun `completeStep upserts progress`() = runTest {
        coEvery { dao.upsertProgress(any()) } returns Unit
        repository.completeStep(5L, 10L)
        coVerify { dao.upsertProgress(match { it.runId == 5L && it.stepId == 10L }) }
    }
}
