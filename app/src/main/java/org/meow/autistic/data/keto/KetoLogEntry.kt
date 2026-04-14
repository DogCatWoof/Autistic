package org.meow.autistic.data.keto

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single food entry in the keto daily food log.
 * [date] is ISO format "yyyy-MM-dd". [netCarbs] and [calories] are derived properties.
 */
@Entity(tableName = "keto_log")
data class KetoLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO date "yyyy-MM-dd" this entry belongs to. */
    val date: String,
    val foodName: String,
    val fat: Double = 0.0,
    val protein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val fiber: Double = 0.0,
    /** Milligrams. */
    val sodium: Double = 0.0,
    /** Fluid ounces. */
    val water: Double = 0.0,
) {
    val netCarbs: Double get() = (totalCarbs - fiber).coerceAtLeast(0.0)
    val calories: Double get() = fat * 9.0 + protein * 4.0 + totalCarbs * 4.0
}
