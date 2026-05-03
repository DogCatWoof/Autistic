package org.meow.autistic.data.sequence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.meow.autistic.data.task.TaskDatabase
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class SequenceDaoTest {

    private lateinit var db: TaskDatabase
    private lateinit var dao: SequenceDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.sequenceDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // region sequences

    @Test
    fun getAll_returnsEmptyInitially() = runTest {
        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun insertSequence_appearsInGetAll() = runTest {
        dao.insertSequence(SequenceEntity(name = "Morning Routine"))
        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("Morning Routine", all[0].name)
    }

    @Test
    fun deleteSequence_removesFromGetAll() = runTest {
        val id = dao.insertSequence(SequenceEntity(name = "Morning Routine"))
        val seq = dao.getById(id)!!
        dao.deleteSequence(seq)
        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun getAll_orderedByNameAscending() = runTest {
        dao.insertSequence(SequenceEntity(name = "Zumba"))
        dao.insertSequence(SequenceEntity(name = "Alpha"))
        dao.insertSequence(SequenceEntity(name = "Morning"))
        val names = dao.getAll().first().map { it.name }
        assertEquals(listOf("Alpha", "Morning", "Zumba"), names)
    }

    // endregion

    // region steps

    @Test
    fun getSteps_returnsEmptyWhenNoSteps() = runTest {
        val seqId = dao.insertSequence(SequenceEntity(name = "Routine"))
        assertTrue(dao.getSteps(seqId).first().isEmpty())
    }

    @Test
    fun insertStep_appearsInGetSteps() = runTest {
        val seqId = dao.insertSequence(SequenceEntity(name = "Routine"))
        dao.insertStep(SequenceStepEntity(sequenceId = seqId, instruction = "Brush teeth", position = 0))
        val steps = dao.getSteps(seqId).first()
        assertEquals(1, steps.size)
        assertEquals("Brush teeth", steps[0].instruction)
    }

    @Test
    fun getSteps_orderedByPosition() = runTest {
        val seqId = dao.insertSequence(SequenceEntity(name = "Routine"))
        dao.insertStep(SequenceStepEntity(sequenceId = seqId, instruction = "Step C", position = 2))
        dao.insertStep(SequenceStepEntity(sequenceId = seqId, instruction = "Step A", position = 0))
        dao.insertStep(SequenceStepEntity(sequenceId = seqId, instruction = "Step B", position = 1))
        val steps = dao.getSteps(seqId).first()
        assertEquals(listOf("Step A", "Step B", "Step C"), steps.map { it.instruction })
    }

    @Test
    fun deleteStepsForSequence_removesAllSteps() = runTest {
        val seqId = dao.insertSequence(SequenceEntity(name = "Routine"))
        dao.insertStep(SequenceStepEntity(sequenceId = seqId, instruction = "Step 1", position = 0))
        dao.insertStep(SequenceStepEntity(sequenceId = seqId, instruction = "Step 2", position = 1))
        dao.deleteStepsForSequence(seqId)
        assertTrue(dao.getSteps(seqId).first().isEmpty())
    }

    // endregion

    // region runs

    @Test
    fun getActiveRun_returnsNullWhenNoRun() = runTest {
        assertNull(dao.getActiveRun().first())
    }

    @Test
    fun insertRun_appearsAsActiveRun() = runTest {
        val seqId = dao.insertSequence(SequenceEntity(name = "Routine"))
        dao.insertRun(SequenceRunEntity(sequenceId = seqId, startedAt = Instant.EPOCH))
        assertNotNull(dao.getActiveRun().first())
    }

    @Test
    fun completedRun_notReturnedByGetActiveRun() = runTest {
        val seqId = dao.insertSequence(SequenceEntity(name = "Routine"))
        val runId = dao.insertRun(SequenceRunEntity(sequenceId = seqId, startedAt = Instant.EPOCH))
        val run = dao.getRunById(runId)!!
        dao.updateRun(run.copy(completedAt = Instant.now()))
        assertNull(dao.getActiveRun().first())
    }

    // endregion

    // region progress

    @Test
    fun getProgress_returnsEmptyWhenNoProgress() = runTest {
        val seqId = dao.insertSequence(SequenceEntity(name = "Routine"))
        val runId = dao.insertRun(SequenceRunEntity(sequenceId = seqId, startedAt = Instant.EPOCH))
        assertTrue(dao.getProgress(runId).first().isEmpty())
    }

    @Test
    fun upsertProgress_appearsInGetProgress() = runTest {
        val seqId = dao.insertSequence(SequenceEntity(name = "Routine"))
        val stepId = dao.insertStep(SequenceStepEntity(sequenceId = seqId, instruction = "Step", position = 0))
        val runId = dao.insertRun(SequenceRunEntity(sequenceId = seqId, startedAt = Instant.EPOCH))
        dao.upsertProgress(SequenceStepProgressEntity(runId, stepId, Instant.EPOCH))
        val progress = dao.getProgress(runId).first()
        assertEquals(1, progress.size)
        assertEquals(stepId, progress[0].stepId)
    }

    @Test
    fun upsertProgress_replacesExistingEntry() = runTest {
        val seqId = dao.insertSequence(SequenceEntity(name = "Routine"))
        val stepId = dao.insertStep(SequenceStepEntity(sequenceId = seqId, instruction = "Step", position = 0))
        val runId = dao.insertRun(SequenceRunEntity(sequenceId = seqId, startedAt = Instant.EPOCH))
        val t1 = Instant.ofEpochMilli(1000L)
        val t2 = Instant.ofEpochMilli(2000L)
        dao.upsertProgress(SequenceStepProgressEntity(runId, stepId, t1))
        dao.upsertProgress(SequenceStepProgressEntity(runId, stepId, t2))
        val progress = dao.getProgress(runId).first()
        assertEquals(1, progress.size)
        assertEquals(t2, progress[0].completedAt)
    }

    // endregion

    // region Firestore sync

    @Test
    fun getPendingFirestoreSync_excludesDeletedSequences() = runTest {
        dao.insertSequence(SequenceEntity(name = "Active", pendingFirestoreSync = true))
        dao.insertSequence(SequenceEntity(name = "Deleted", pendingFirestoreSync = true, isDeleted = true))
        val result = dao.getPendingFirestoreSync()
        assertEquals(1, result.size)
        assertEquals("Active", result[0].name)
    }

    @Test
    fun getPendingFirestoreDelete_returnsOnlyDeletedSequences() = runTest {
        dao.insertSequence(SequenceEntity(name = "Active", pendingFirestoreSync = true))
        dao.insertSequence(SequenceEntity(name = "Deleted", pendingFirestoreSync = true, isDeleted = true))
        val result = dao.getPendingFirestoreDelete()
        assertEquals(1, result.size)
        assertEquals("Deleted", result[0].name)
    }

    @Test
    fun markSequenceFirestoreSynced_clearsFlagAndSetsId() = runTest {
        val id = dao.insertSequence(SequenceEntity(name = "Routine", pendingFirestoreSync = true))
        dao.markSequenceFirestoreSynced(id, "fs-seq-1")
        val result = dao.getById(id)
        assertEquals("fs-seq-1", result?.firestoreId)
        assertEquals(false, result?.pendingFirestoreSync)
    }

    @Test
    fun getPendingFirestoreStepSync_excludesDeletedSteps() = runTest {
        val seqId = dao.insertSequence(SequenceEntity(name = "Routine"))
        dao.insertStep(SequenceStepEntity(sequenceId = seqId, instruction = "Active", position = 0, pendingFirestoreSync = true))
        dao.insertStep(SequenceStepEntity(sequenceId = seqId, instruction = "Deleted", position = 1, pendingFirestoreSync = true, isDeleted = true))
        val result = dao.getPendingFirestoreStepSync()
        assertEquals(1, result.size)
        assertEquals("Active", result[0].instruction)
    }

    @Test
    fun getPendingFirestoreRunSync_returnsOnlyPendingRuns() = runTest {
        val seqId = dao.insertSequence(SequenceEntity(name = "Routine"))
        dao.insertRun(SequenceRunEntity(sequenceId = seqId, startedAt = Instant.EPOCH, pendingFirestoreSync = true))
        dao.insertRun(SequenceRunEntity(sequenceId = seqId, startedAt = Instant.EPOCH, pendingFirestoreSync = false))
        val result = dao.getPendingFirestoreRunSync()
        assertEquals(1, result.size)
    }

    @Test
    fun markRunFirestoreSynced_clearsFlagAndSetsId() = runTest {
        val seqId = dao.insertSequence(SequenceEntity(name = "Routine"))
        val runId = dao.insertRun(SequenceRunEntity(sequenceId = seqId, startedAt = Instant.EPOCH, pendingFirestoreSync = true))
        dao.markRunFirestoreSynced(runId, "fs-run-1")
        val result = dao.getRunById(runId)
        assertEquals("fs-run-1", result?.firestoreId)
        assertEquals(false, result?.pendingFirestoreSync)
    }

    // endregion
}
