package org.meow.autistic.data.keto

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Daily aggregate of all [KetoItemEntry] records for a given date.
 * Derived by the ViewModel; not written directly.
 *
 * Net Carbs = Total Carbs − Dietary Fiber − Sugar Alcohols.
 */
@Entity(tableName = "keto_log")
data class KetoLogEntry(
    @PrimaryKey val date: String,
    val calories: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val fiber: Double = 0.0,
    val totalSugars: Double = 0.0,
    val addedSugars: Double = 0.0,
    val sugarAlcohols: Double = 0.0,
) {
    val netCarbs: Double get() = (totalCarbs - fiber - sugarAlcohols).coerceAtLeast(0.0)
}
