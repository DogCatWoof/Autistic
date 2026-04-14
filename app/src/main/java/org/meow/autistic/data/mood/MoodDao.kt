package org.meow.autistic.data.mood

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** DAO for mood readings. */
@Dao
interface MoodDao {
    @Query("SELECT * FROM moods ORDER BY createdAt DESC")
    fun getAll(): Flow<List<MoodEntity>>

    @Insert
    suspend fun insert(mood: MoodEntity)

    @Delete
    suspend fun delete(mood: MoodEntity)
}
