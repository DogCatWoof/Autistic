package org.meow.autistic.data.keto

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KetoDao {
    @Query("SELECT * FROM keto_log WHERE date = :date")
    fun getByDate(date: String): Flow<KetoLogEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: KetoLogEntry)

    /** Deletes all records with a date strictly before [cutoffDate] (ISO "yyyy-MM-dd"). */
    @Query("DELETE FROM keto_log WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)
}
