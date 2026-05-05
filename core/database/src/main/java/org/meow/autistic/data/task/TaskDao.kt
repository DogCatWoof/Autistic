package org.meow.autistic.data.task

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity): Int

    @Delete
    suspend fun deleteTask(task: TaskEntity): Int

    @Query("SELECT * FROM tasks WHERE syncStatus = 'pending_push' OR (syncStatus = 'local' AND dailyTaskId IS NULL)")
    suspend fun getPendingPush(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE syncStatus = 'pending_delete'")
    suspend fun getPendingDelete(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE googleTaskId = :googleTaskId LIMIT 1")
    suspend fun getByGoogleTaskId(googleTaskId: String): TaskEntity?

    @Query("UPDATE tasks SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String): Int

    @Query("UPDATE tasks SET syncStatus = 'pending_delete', pendingFirestoreSync = 1, lastModifiedAt = :modifiedAt WHERE id = :id")
    suspend fun markPendingDeleteSync(id: Long, modifiedAt: Instant)

    @Query("UPDATE tasks SET syncStatus = 'synced', googleTaskId = :googleTaskId, lastSyncedAt = :lastSyncedAt WHERE id = :id")
    suspend fun markSynced(id: Long, googleTaskId: String, lastSyncedAt: Instant): Int

    @Query("DELETE FROM tasks WHERE googleTaskId IN (:googleTaskIds)")
    suspend fun deleteByGoogleTaskIds(googleTaskIds: List<String>): Int

    /** Removes all incomplete tasks that were generated from daily task templates. */
    @Query("DELETE FROM tasks WHERE dailyTaskId IS NOT NULL AND completedAt IS NULL")
    suspend fun deleteUnfinishedDailyTasks(): Int

    @Query("DELETE FROM tasks WHERE completedAt IS NOT NULL AND completedAt < :cutoff")
    suspend fun deleteStaleCompleted(cutoff: Instant): Int

    @Query("SELECT * FROM tasks WHERE completedAt IS NOT NULL ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE pendingFirestoreSync = 1 AND syncStatus != 'pending_delete'")
    suspend fun getPendingFirestoreSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE pendingFirestoreSync = 1 AND syncStatus = 'pending_delete'")
    suspend fun getPendingFirestoreDelete(): List<TaskEntity>

    @Query("UPDATE tasks SET pendingFirestoreSync = 0, firestoreId = :firestoreId WHERE id = :id")
    suspend fun markFirestoreSynced(id: Long, firestoreId: String)

    @Query("SELECT * FROM tasks WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getByFirestoreId(firestoreId: String): TaskEntity?
}
