package org.meow.autistic.data.energy

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Start-of-day wellness check; [baselineMultiplier] scales today's effective capacity. */
@Entity(tableName = "start_of_day")
data class StartOfDayEntry(
    @PrimaryKey val date: String,
    val sleepQuality: Int,
    val stressLevel: Int,
    val physicalState: Int,
    val baselineMultiplier: Double,
)
