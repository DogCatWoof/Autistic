package org.meow.autistic.data.energy

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Per-activity energy cost, refined over time via end-of-block difficulty ratings. */
@Entity(tableName = "activity_cost")
data class ActivityCostEntry(
    @PrimaryKey val activityType: String,
    val baseCost: Double,
    val learnedCost: Double,
    val sampleCount: Int = 0,
)
