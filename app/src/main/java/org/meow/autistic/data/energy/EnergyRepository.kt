package org.meow.autistic.data.energy

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * Source of truth for energy budget data.
 * Balance = effective capacity − sum of today's log costs.
 * Effective capacity = dailyCapacity × startOfDay.baselineMultiplier (default 1.0).
 */
class EnergyRepository(private val dao: EnergyDao) {

    suspend fun getOrCreateProfile(): EnergyProfileEntity {
        return dao.getProfile() ?: EnergyProfileEntity().also { dao.upsertProfile(it) }
    }

    suspend fun updateProfile(profile: EnergyProfileEntity) = dao.upsertProfile(profile)

    fun getTodayLogs(): Flow<List<EnergyLogEntry>> =
        dao.getLogsByDate(LocalDate.now().toString())

    suspend fun getTodayCapacity(): Double {
        val profile = getOrCreateProfile()
        val startOfDay = dao.getStartOfDay(LocalDate.now().toString())
        return profile.dailyCapacity * (startOfDay?.baselineMultiplier ?: 1.0)
    }

    suspend fun getTodaySpent(): Double =
        dao.getLogsByDateOnce(LocalDate.now().toString()).sumOf { it.energyCost }

    suspend fun getTodayBalance(): Double = getTodayCapacity() - getTodaySpent()

    suspend fun projectBalance(activityType: String): Double {
        val cost = dao.getCost(activityType)?.learnedCost ?: (DEFAULT_COSTS[activityType] ?: 2.0)
        return getTodayBalance() - cost
    }

    suspend fun logActivity(activityType: String, description: String?, difficulty: Int?): Long {
        val cost = dao.getCost(activityType)?.learnedCost ?: (DEFAULT_COSTS[activityType] ?: 2.0)
        val entry = EnergyLogEntry(
            date = LocalDate.now().toString(),
            activityType = activityType,
            description = description,
            loggedAt = Instant.now(),
            reportedDifficulty = difficulty,
            energyCost = cost,
        )
        return dao.insertLog(entry)
    }

    suspend fun deleteLog(entry: EnergyLogEntry) = dao.deleteLog(entry)

    suspend fun upsertStartOfDay(entry: StartOfDayEntry) = dao.upsertStartOfDay(entry)

    suspend fun getTodayStartOfDay(): StartOfDayEntry? =
        dao.getStartOfDay(LocalDate.now().toString())

    suspend fun calibrateDay(date: String) {
        for (log in dao.getLogsByDateOnce(date)) {
            val difficultyMultiplier = when (log.reportedDifficulty) {
                1 -> 0.85
                3 -> 1.2
                else -> 1.0
            }
            val actual = log.energyCost * difficultyMultiplier
            val existing = dao.getCost(log.activityType)
            if (existing != null) {
                val n = (existing.sampleCount + 1).coerceAtMost(50)
                val updated = existing.learnedCost + (actual - existing.learnedCost) / n
                dao.upsertCost(existing.copy(learnedCost = updated, sampleCount = n))
            } else {
                dao.upsertCost(ActivityCostEntry(log.activityType, actual, actual, 1))
            }
        }
    }

    companion object {
        val DEFAULT_COSTS: Map<String, Double> = linkedMapOf(
            "Focus Work" to 2.0,
            "Meeting" to 3.0,
            "Social" to 2.5,
            "Phone Call" to 1.5,
            "Errand" to 1.5,
            "Exercise" to 2.0,
            "Transition" to 1.0,
            "Admin" to 1.0,
            "Sensory" to 2.0,
            "Break" to -1.0,
            "Nap" to -2.0,
        )
    }
}
