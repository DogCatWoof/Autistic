package org.meow.autistic.data.energy

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EnergyDao {
    @Upsert suspend fun upsertProfile(profile: EnergyProfileEntity)
    @Query("SELECT * FROM energy_profile WHERE id = 1")
    suspend fun getProfile(): EnergyProfileEntity?

    @Insert suspend fun insertLog(entry: EnergyLogEntry): Long
    @Delete suspend fun deleteLog(entry: EnergyLogEntry)
    @Query("SELECT * FROM energy_log WHERE date = :date ORDER BY loggedAt DESC")
    fun getLogsByDate(date: String): Flow<List<EnergyLogEntry>>
    @Query("SELECT * FROM energy_log WHERE date = :date ORDER BY loggedAt DESC")
    suspend fun getLogsByDateOnce(date: String): List<EnergyLogEntry>

    @Upsert suspend fun upsertCost(cost: ActivityCostEntry)
    @Query("SELECT * FROM activity_cost WHERE activityType = :type")
    suspend fun getCost(type: String): ActivityCostEntry?

    @Upsert suspend fun upsertStartOfDay(entry: StartOfDayEntry)
    @Query("SELECT * FROM start_of_day WHERE date = :date")
    suspend fun getStartOfDay(date: String): StartOfDayEntry?
}
