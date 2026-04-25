package org.meow.autistic.data.energy

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** One logged activity entry; [energyCost] is negative for recovery activities. */
@Entity(tableName = "energy_log")
data class EnergyLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val activityType: String,
    val description: String? = null,
    val loggedAt: Instant = Instant.now(),
    val reportedDifficulty: Int? = null,
    val energyCost: Double,
)
