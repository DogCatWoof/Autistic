package org.meow.autistic.data.energy

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Singleton (id=1) storing the user's daily energy capacity and budgeting mode. */
@Entity(tableName = "energy_profile")
data class EnergyProfileEntity(
    @PrimaryKey val id: Int = 1,
    val dailyCapacity: Int = 10,
    val mode: String = "Learning",
)
