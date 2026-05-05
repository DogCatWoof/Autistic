package org.meow.autistic.data.foodlog

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Daily aggregate of all [FoodLogItemEntry] records for a given date.
 * Derived by the ViewModel; not written directly.
 *
 * Net Carbs = Total Carbs − Dietary Fiber − Sugar Alcohols.
 */
@Entity(tableName = "food_log")
data class FoodLogEntry(
    @PrimaryKey val date: String,
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val fiber: Double = 0.0,
    val totalSugars: Double = 0.0,
    val addedSugars: Double = 0.0,
    val sugarAlcohols: Double = 0.0,
) {
    val netCarbs: Double get() = (totalCarbs - fiber - sugarAlcohols).coerceAtLeast(0.0)
    val proteinCalories: Double get() = protein * 4.0
    val fatCalories: Double get() = totalFat * 9.0
    val netCarbCalories: Double get() = netCarbs * 4.0
}
