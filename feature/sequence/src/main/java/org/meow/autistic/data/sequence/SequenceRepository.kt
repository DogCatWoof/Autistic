package org.meow.autistic.data.sequence

import android.os.SystemClock
import kotlinx.coroutines.flow.Flow
import org.meow.autistic.data.diagnostics.QueryLogger
import java.time.Instant

/** Persistence layer for sequences, steps, runs, and step progress. */
class SequenceRepository(
    private val dao: SequenceDao,
    private val queryLogger: QueryLogger,
) {
    fun getAllSequences(): Flow<List<SequenceEntity>> = dao.getAll()
    fun getSteps(sequenceId: Long): Flow<List<SequenceStepEntity>> = dao.getSteps(sequenceId)
    fun getActiveRun(): Flow<SequenceRunEntity?> = dao.getActiveRun()
    fun getProgress(runId: Long): Flow<List<SequenceStepProgressEntity>> = dao.getProgress(runId)

    suspend fun getById(id: Long): SequenceEntity? =
        timed("SequenceRepository.getById") { dao.getById(id) }

    suspend fun insertSequence(sequence: SequenceEntity): Long =
        timed("SequenceRepository.insertSequence") {
            dao.insertSequence(sequence.copy(lastModifiedAt = Instant.now(), pendingFirestoreSync = true))
        }

    suspend fun deleteSequence(sequence: SequenceEntity) =
        timed("SequenceRepository.deleteSequence") {
            val now = Instant.now()
            for (step in dao.getStepsOnce(sequence.id)) {
                dao.upsertStep(step.copy(isDeleted = true, lastModifiedAt = now, pendingFirestoreSync = true))
            }
            dao.upsertSequence(sequence.copy(isDeleted = true, lastModifiedAt = now, pendingFirestoreSync = true))
        }

    suspend fun insertStep(step: SequenceStepEntity): Long =
        timed("SequenceRepository.insertStep") {
            dao.insertStep(step.copy(lastModifiedAt = Instant.now(), pendingFirestoreSync = true))
        }

    suspend fun deleteStep(step: SequenceStepEntity) =
        timed("SequenceRepository.deleteStep") {
            dao.upsertStep(step.copy(isDeleted = true, lastModifiedAt = Instant.now(), pendingFirestoreSync = true))
        }

    suspend fun getStepsOnce(sequenceId: Long): List<SequenceStepEntity> =
        timed("SequenceRepository.getStepsOnce") { dao.getStepsOnce(sequenceId) }

    suspend fun startRun(sequenceId: Long): Long =
        timed("SequenceRepository.startRun") {
            dao.insertRun(SequenceRunEntity(sequenceId = sequenceId, startedAt = Instant.now()))
        }

    suspend fun completeRun(runId: Long) =
        timed("SequenceRepository.completeRun") {
            val run = dao.getRunById(runId)
                ?: throw IllegalStateException("Run $runId not found")
            val now = Instant.now()
            dao.updateRun(run.copy(completedAt = now, lastModifiedAt = now, pendingFirestoreSync = true))
        }

    suspend fun completeStep(runId: Long, stepId: Long) =
        timed("SequenceRepository.completeStep") {
            dao.upsertProgress(SequenceStepProgressEntity(runId, stepId, Instant.now()))
        }

    suspend fun getRunById(runId: Long): SequenceRunEntity? =
        timed("SequenceRepository.getRunById") { dao.getRunById(runId) }

    suspend fun getActiveRunOnce(): SequenceRunEntity? =
        timed("SequenceRepository.getActiveRunOnce") { dao.getActiveRunOnce() }

    suspend fun getProgressOnce(runId: Long): List<SequenceStepProgressEntity> =
        timed("SequenceRepository.getProgressOnce") { dao.getProgressOnce(runId) }

    private suspend inline fun <T> timed(label: String, block: suspend () -> T): T {
        val start = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            queryLogger.log(label, SystemClock.elapsedRealtime() - start)
        }
    }
}
