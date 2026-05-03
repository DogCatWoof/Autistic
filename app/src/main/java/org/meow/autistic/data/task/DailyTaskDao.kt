package org.meow.autistic.data.task

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Data-access object for [DailyTaskEntity]. */
@Dao
interface DailyTaskDao {

    @Query("SELECT * FROM daily_tasks ORDER BY title ASC")
    fun getAll(): Flow<List<DailyTaskEntity>>

    @Query("SELECT * FROM daily_tasks ORDER BY title ASC")
    suspend fun getAllOnce(): List<DailyTaskEntity>

    @Insert
    suspend fun insert(task: DailyTaskEntity): Long

    @Update
    suspend fun update(task: DailyTaskEntity)

    @Delete
    suspend fun delete(task: DailyTaskEntity)

    @Query("SELECT * FROM daily_tasks WHERE pendingFirestoreSync = 1")
    suspend fun getPendingFirestoreSync(): List<DailyTaskEntity>

    @Query("UPDATE daily_tasks SET pendingFirestoreSync = 0, firestoreId = :firestoreId WHERE id = :id")
    suspend fun markFirestoreSynced(id: Long, firestoreId: String)
}
