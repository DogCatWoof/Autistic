package org.meow.autistic.data.keto

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One food item logged for a specific calendar day.
 * Net Carbs = Total Carbs − Dietary Fiber − Sugar Alcohols.
 *
 * @param date ISO date string ("yyyy-MM-dd").
 * @param name Display name of the food or meal.
 */
@Entity(tableName = "keto_items")
data class KetoItemEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val name: String,
    val totalCarbs: Double = 0.0,
    val fiber: Double = 0.0,
    val totalSugars: Double = 0.0,
    val addedSugars: Double = 0.0,
    val sugarAlcohols: Double = 0.0,
) {
    val netCarbs: Double get() = (totalCarbs - fiber - sugarAlcohols).coerceAtLeast(0.0)
}
