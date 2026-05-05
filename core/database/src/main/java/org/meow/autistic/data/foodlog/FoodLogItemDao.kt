package org.meow.autistic.data.foodlog

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogItemDao {
    @Query("SELECT * FROM food_log_items WHERE date = :date AND isDeleted = 0 ORDER BY loggedAt ASC")
    fun getByDate(date: String): Flow<List<FoodLogItemEntry>>

    @Insert
    suspend fun insert(entry: FoodLogItemEntry): Long

    @Update
    suspend fun update(entry: FoodLogItemEntry)

    @Delete
    suspend fun delete(entry: FoodLogItemEntry)

    @Query("SELECT * FROM food_log_items WHERE id = :id")
    suspend fun getById(id: Long): FoodLogItemEntry?

    @Query("SELECT * FROM food_log_items WHERE isAiPending = 1 AND isDeleted = 0 ORDER BY loggedAt ASC")
    suspend fun getPendingAnalysis(): List<FoodLogItemEntry>

    @Query("DELETE FROM food_log_items WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)

    @Query("SELECT * FROM food_log_items WHERE pendingFirestoreSync = 1 AND isDeleted = 0")
    suspend fun getPendingFirestoreSync(): List<FoodLogItemEntry>

    @Query("SELECT * FROM food_log_items WHERE pendingFirestoreSync = 1 AND isDeleted = 1")
    suspend fun getPendingFirestoreDelete(): List<FoodLogItemEntry>

    @Query("UPDATE food_log_items SET pendingFirestoreSync = 0, firestoreId = :firestoreId WHERE id = :id")
    suspend fun markFirestoreSynced(id: Long, firestoreId: String)

    @Query("SELECT * FROM food_log_items WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getByFirestoreId(firestoreId: String): FoodLogItemEntry?

    @Upsert
    suspend fun upsert(entry: FoodLogItemEntry)
}
