package org.meow.autistic.data.todo

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM tasks WHERE syncStatus = 'pending_push'")
    suspend fun getPendingPush(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE syncStatus = 'pending_delete'")
    suspend fun getPendingDelete(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE googleTaskId = :googleTaskId LIMIT 1")
    suspend fun getByGoogleTaskId(googleTaskId: String): TaskEntity?

    @Query("UPDATE tasks SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String): Int

    @Query("UPDATE tasks SET syncStatus = 'synced', googleTaskId = :googleTaskId, lastSyncedAt = :lastSyncedAt WHERE id = :id")
    suspend fun markSynced(id: Long, googleTaskId: String, lastSyncedAt: Long): Int

    @Query("DELETE FROM tasks WHERE googleTaskId IN (:googleTaskIds)")
    suspend fun deleteByGoogleTaskIds(googleTaskIds: List<String>): Int

    /** Removes all incomplete tasks that were generated from daily task templates. */
    @Query("DELETE FROM tasks WHERE dailyTaskId IS NOT NULL AND isCompleted = 0")
    suspend fun deleteUnfinishedDailyTasks(): Int

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteAllCompleted(): Int
}
