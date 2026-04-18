package org.meow.autistic.data.keto

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KetoItemDao {
    @Query("SELECT * FROM keto_items WHERE date = :date ORDER BY loggedAt ASC")
    fun getByDate(date: String): Flow<List<KetoItemEntry>>

    @Insert
    suspend fun insert(entry: KetoItemEntry): Long

    @Delete
    suspend fun delete(entry: KetoItemEntry)

    @Query("DELETE FROM keto_items WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)
}
