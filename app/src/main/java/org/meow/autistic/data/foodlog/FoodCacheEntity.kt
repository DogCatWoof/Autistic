package org.meow.autistic.data.foodlog

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores per-serving nutrition values for a named food, populated when the user accepts
 * an AI-analysed photo entry. Used to pre-fill future analyses of the same food.
 */
@Entity(tableName = "food_cache")
data class FoodCacheEntity(
    @PrimaryKey val description: String,
    val calories: Double,
    val protein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val fiber: Double,
    val totalSugars: Double,
    val addedSugars: Double,
    val sugarAlcohols: Double,
)
