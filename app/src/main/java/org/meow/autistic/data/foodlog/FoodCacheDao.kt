package org.meow.autistic.data.foodlog

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface FoodCacheDao {
    @Upsert
    suspend fun upsert(entry: FoodCacheEntity)

    @Query("SELECT * FROM food_cache WHERE LOWER(description) = LOWER(:description) LIMIT 1")
    suspend fun findByDescription(description: String): FoodCacheEntity?
}
