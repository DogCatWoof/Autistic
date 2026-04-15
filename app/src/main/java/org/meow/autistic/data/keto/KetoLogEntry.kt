package org.meow.autistic.data.keto

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One record per calendar day, keyed by ISO date ("yyyy-MM-dd").
 * Sub-components of Total Carbs follow nutrition-label hierarchy:
 *   Total Carbs ⊇ Dietary Fiber, Total Sugars (⊇ Added Sugars), Sugar Alcohols
 * Net Carbs (keto) = Total Carbs − Dietary Fiber − Sugar Alcohols.
 */
@Entity(tableName = "keto_log")
data class KetoLogEntry(
    @PrimaryKey val date: String,
    val fat: Double = 0.0,
    val protein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val fiber: Double = 0.0,
    val totalSugars: Double = 0.0,
    val addedSugars: Double = 0.0,
    val sugarAlcohols: Double = 0.0,
) {
    val netCarbs: Double get() = (totalCarbs - fiber - sugarAlcohols).coerceAtLeast(0.0)
}
