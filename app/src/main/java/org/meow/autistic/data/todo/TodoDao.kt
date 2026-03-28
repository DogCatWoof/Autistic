package org.meow.autistic.data.todo

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY createdAt DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoEntity): Int

    @Delete
    suspend fun deleteTodo(todo: TodoEntity): Int

    @Query("SELECT * FROM todos WHERE syncStatus = 'pending_push'")
    suspend fun getPendingPush(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE syncStatus = 'pending_delete'")
    suspend fun getPendingDelete(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE googleTaskId = :googleTaskId LIMIT 1")
    suspend fun getByGoogleTaskId(googleTaskId: String): TodoEntity?

    @Query("UPDATE todos SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String): Int

    @Query("UPDATE todos SET syncStatus = 'synced', googleTaskId = :googleTaskId, lastSyncedAt = :lastSyncedAt WHERE id = :id")
    suspend fun markSynced(id: Long, googleTaskId: String, lastSyncedAt: Long): Int

    @Query("DELETE FROM todos WHERE googleTaskId IN (:googleTaskIds)")
    suspend fun deleteByGoogleTaskIds(googleTaskIds: List<String>): Int

    /** Removes all incomplete todos that were generated from daily task templates. */
    @Query("DELETE FROM todos WHERE dailyTaskId IS NOT NULL AND isCompleted = 0")
    suspend fun deleteUnfinishedDailyTodos(): Int
}
