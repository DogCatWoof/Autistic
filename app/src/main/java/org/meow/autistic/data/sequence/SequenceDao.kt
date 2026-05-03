package org.meow.autistic.data.sequence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Data access for sequences, steps, runs, and step-completion progress. */
@Dao
interface SequenceDao {

    @Query("SELECT * FROM sequences ORDER BY name ASC")
    fun getAll(): Flow<List<SequenceEntity>>

    @Query("SELECT * FROM sequences WHERE id = :id")
    suspend fun getById(id: Long): SequenceEntity?

    @Insert
    suspend fun insertSequence(sequence: SequenceEntity): Long

    @Delete
    suspend fun deleteSequence(sequence: SequenceEntity)

    @Query("SELECT * FROM sequence_steps WHERE sequenceId = :sequenceId ORDER BY position ASC")
    fun getSteps(sequenceId: Long): Flow<List<SequenceStepEntity>>

    @Query("SELECT * FROM sequence_steps WHERE sequenceId = :sequenceId ORDER BY position ASC")
    suspend fun getStepsOnce(sequenceId: Long): List<SequenceStepEntity>

    @Query("SELECT * FROM sequence_steps WHERE id = :id")
    suspend fun getStepById(id: Long): SequenceStepEntity?

    @Insert
    suspend fun insertStep(step: SequenceStepEntity): Long

    @Delete
    suspend fun deleteStep(step: SequenceStepEntity)

    @Query("DELETE FROM sequence_steps WHERE sequenceId = :sequenceId")
    suspend fun deleteStepsForSequence(sequenceId: Long)

    @Insert
    suspend fun insertRun(run: SequenceRunEntity): Long

    @Update
    suspend fun updateRun(run: SequenceRunEntity)

    @Query("SELECT * FROM sequence_runs WHERE completedAt IS NULL LIMIT 1")
    fun getActiveRun(): Flow<SequenceRunEntity?>

    @Query("SELECT * FROM sequence_runs WHERE completedAt IS NULL LIMIT 1")
    suspend fun getActiveRunOnce(): SequenceRunEntity?

    @Query("SELECT * FROM sequence_runs WHERE id = :id")
    suspend fun getRunById(id: Long): SequenceRunEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: SequenceStepProgressEntity)

    @Query("SELECT * FROM sequence_step_progress WHERE runId = :runId")
    fun getProgress(runId: Long): Flow<List<SequenceStepProgressEntity>>

    @Query("SELECT * FROM sequence_step_progress WHERE runId = :runId")
    suspend fun getProgressOnce(runId: Long): List<SequenceStepProgressEntity>

    @Query("SELECT * FROM sequences WHERE pendingFirestoreSync = 1 AND isDeleted = 0")
    suspend fun getPendingFirestoreSync(): List<SequenceEntity>

    @Query("SELECT * FROM sequences WHERE pendingFirestoreSync = 1 AND isDeleted = 1")
    suspend fun getPendingFirestoreDelete(): List<SequenceEntity>

    @Query("UPDATE sequences SET pendingFirestoreSync = 0, firestoreId = :firestoreId WHERE id = :id")
    suspend fun markSequenceFirestoreSynced(id: Long, firestoreId: String)

    @Query("SELECT * FROM sequence_steps WHERE pendingFirestoreSync = 1 AND isDeleted = 0")
    suspend fun getPendingFirestoreStepSync(): List<SequenceStepEntity>

    @Query("SELECT * FROM sequence_steps WHERE pendingFirestoreSync = 1 AND isDeleted = 1")
    suspend fun getPendingFirestoreStepDelete(): List<SequenceStepEntity>

    @Query("UPDATE sequence_steps SET pendingFirestoreSync = 0, firestoreId = :firestoreId WHERE id = :id")
    suspend fun markStepFirestoreSynced(id: Long, firestoreId: String)

    @Query("SELECT * FROM sequence_runs WHERE pendingFirestoreSync = 1")
    suspend fun getPendingFirestoreRunSync(): List<SequenceRunEntity>

    @Query("UPDATE sequence_runs SET pendingFirestoreSync = 0, firestoreId = :firestoreId WHERE id = :id")
    suspend fun markRunFirestoreSynced(id: Long, firestoreId: String)
}
