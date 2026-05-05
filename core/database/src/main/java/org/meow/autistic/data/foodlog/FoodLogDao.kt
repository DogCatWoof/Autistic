package org.meow.autistic.data.foodlog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_log WHERE date = :date")
    fun getByDate(date: String): Flow<FoodLogEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: FoodLogEntry)

    /** Deletes all records with a date strictly before [cutoffDate] (ISO "yyyy-MM-dd"). */
    @Query("DELETE FROM food_log WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)
}
