package org.meow.autistic.data.keto

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KetoDao {
    @Query("SELECT * FROM keto_log WHERE date = :date ORDER BY id DESC")
    fun getByDate(date: String): Flow<List<KetoLogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: KetoLogEntry): Long

    @Delete
    suspend fun delete(entry: KetoLogEntry): Int
}
