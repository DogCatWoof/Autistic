package org.meow.autistic.data.energy

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class EnergyRepositoryTest {

    private val dao = mockk<EnergyDao>(relaxed = true)
    private lateinit var repository: EnergyRepository

    @Before
    fun setUp() {
        repository = EnergyRepository(dao)
    }

    // region getOrCreateProfile

    @Test
    fun `getOrCreateProfile returns existing profile`() = runTest {
        val profile = EnergyProfileEntity(dailyCapacity = 8)
        coEvery { dao.getProfile() } returns profile
        assertEquals(profile, repository.getOrCreateProfile())
    }

    @Test
    fun `getOrCreateProfile creates and persists default profile when none exists`() = runTest {
        coEvery { dao.getProfile() } returns null
        val result = repository.getOrCreateProfile()
        assertEquals(10, result.dailyCapacity)
        coVerify { dao.upsertProfile(any()) }
    }

    // endregion

    // region getTodayCapacity

    @Test
    fun `getTodayCapacity uses multiplier 1_0 when no start-of-day check`() = runTest {
        coEvery { dao.getProfile() } returns EnergyProfileEntity(dailyCapacity = 10)
        coEvery { dao.getStartOfDay(any()) } returns null
        assertEquals(10.0, repository.getTodayCapacity(), 0.001)
    }

    @Test
    fun `getTodayCapacity multiplies by start-of-day baseline`() = runTest {
        coEvery { dao.getProfile() } returns EnergyProfileEntity(dailyCapacity = 10)
        coEvery { dao.getStartOfDay(any()) } returns
            StartOfDayEntry("2025-01-01", 2, 4, 2, baselineMultiplier = 0.8)
        assertEquals(8.0, repository.getTodayCapacity(), 0.001)
    }

    @Test
    fun `getTodayCapacity reflects custom profile capacity`() = runTest {
        coEvery { dao.getProfile() } returns EnergyProfileEntity(dailyCapacity = 6)
        coEvery { dao.getStartOfDay(any()) } returns
            StartOfDayEntry("2025-01-01", 3, 3, 3, baselineMultiplier = 1.0)
        assertEquals(6.0, repository.getTodayCapacity(), 0.001)
    }

    // endregion

    // region getTodayBalance

    @Test
    fun `getTodayBalance equals capacity when no logs`() = runTest {
        coEvery { dao.getProfile() } returns EnergyProfileEntity(dailyCapacity = 10)
        coEvery { dao.getStartOfDay(any()) } returns null
        coEvery { dao.getLogsByDateOnce(any()) } returns emptyList()
        assertEquals(10.0, repository.getTodayBalance(), 0.001)
    }

    @Test
    fun `getTodayBalance subtracts sum of activity costs`() = runTest {
        coEvery { dao.getProfile() } returns EnergyProfileEntity(dailyCapacity = 10)
        coEvery { dao.getStartOfDay(any()) } returns null
        coEvery { dao.getLogsByDateOnce(any()) } returns listOf(
            EnergyLogEntry(date = "2025-01-01", activityType = "Focus Work", energyCost = 2.0),
            EnergyLogEntry(date = "2025-01-01", activityType = "Meeting", energyCost = 3.0),
        )
        assertEquals(5.0, repository.getTodayBalance(), 0.001)
    }

    @Test
    fun `getTodayBalance adds back negative-cost recovery entries`() = runTest {
        coEvery { dao.getProfile() } returns EnergyProfileEntity(dailyCapacity = 10)
        coEvery { dao.getStartOfDay(any()) } returns null
        coEvery { dao.getLogsByDateOnce(any()) } returns listOf(
            EnergyLogEntry(date = "2025-01-01", activityType = "Focus Work", energyCost = 3.0),
            EnergyLogEntry(date = "2025-01-01", activityType = "Break", energyCost = -1.0),
        )
        assertEquals(8.0, repository.getTodayBalance(), 0.001)
    }

    // endregion

    // region projectBalance

    @Test
    fun `projectBalance uses learned cost when ActivityCostEntry exists`() = runTest {
        coEvery { dao.getProfile() } returns EnergyProfileEntity(dailyCapacity = 10)
        coEvery { dao.getStartOfDay(any()) } returns null
        coEvery { dao.getLogsByDateOnce(any()) } returns emptyList()
        coEvery { dao.getCost("Focus Work") } returns
            ActivityCostEntry("Focus Work", 2.0, learnedCost = 1.5, sampleCount = 5)
        assertEquals(8.5, repository.projectBalance("Focus Work"), 0.001)
    }

    @Test
    fun `projectBalance falls back to DEFAULT_COSTS when no learned entry`() = runTest {
        coEvery { dao.getProfile() } returns EnergyProfileEntity(dailyCapacity = 10)
        coEvery { dao.getStartOfDay(any()) } returns null
        coEvery { dao.getLogsByDateOnce(any()) } returns emptyList()
        coEvery { dao.getCost("Meeting") } returns null
        assertEquals(7.0, repository.projectBalance("Meeting"), 0.001) // DEFAULT = 3.0
    }

    @Test
    fun `projectBalance uses 2_0 default for completely unknown activity`() = runTest {
        coEvery { dao.getProfile() } returns EnergyProfileEntity(dailyCapacity = 10)
        coEvery { dao.getStartOfDay(any()) } returns null
        coEvery { dao.getLogsByDateOnce(any()) } returns emptyList()
        coEvery { dao.getCost("Unknown") } returns null
        assertEquals(8.0, repository.projectBalance("Unknown"), 0.001)
    }

    // endregion

    // region logActivity

    @Test
    fun `logActivity inserts entry using learned cost`() = runTest {
        coEvery { dao.getCost("Focus Work") } returns
            ActivityCostEntry("Focus Work", 2.0, learnedCost = 1.8, sampleCount = 10)
        repository.logActivity("Focus Work", "Deep work", 2)
        coVerify { dao.insertLog(match { it.activityType == "Focus Work" && it.energyCost == 1.8 }) }
    }

    @Test
    fun `logActivity falls back to DEFAULT_COSTS when no learned entry`() = runTest {
        coEvery { dao.getCost("Social") } returns null
        repository.logActivity("Social", null, null)
        coVerify { dao.insertLog(match { it.activityType == "Social" && it.energyCost == 2.5 }) }
    }

    @Test
    fun `logActivity records description and difficulty`() = runTest {
        coEvery { dao.getCost(any()) } returns null
        repository.logActivity("Errand", "Grocery run", 3)
        coVerify {
            dao.insertLog(match { it.description == "Grocery run" && it.reportedDifficulty == 3 })
        }
    }

    // endregion

    // region calibrateDay

    @Test
    fun `calibrateDay is no-op when date has no logs`() = runTest {
        coEvery { dao.getLogsByDateOnce(any()) } returns emptyList()
        repository.calibrateDay("2025-01-01")
        coVerify(exactly = 0) { dao.upsertCost(any()) }
    }

    @Test
    fun `calibrateDay creates new ActivityCostEntry for unknown activity`() = runTest {
        val log = EnergyLogEntry(
            date = "2025-01-01", activityType = "Focus Work",
            energyCost = 2.0, reportedDifficulty = 2,
        )
        coEvery { dao.getLogsByDateOnce("2025-01-01") } returns listOf(log)
        coEvery { dao.getCost("Focus Work") } returns null
        repository.calibrateDay("2025-01-01")
        coVerify { dao.upsertCost(ActivityCostEntry("Focus Work", 2.0, 2.0, 1)) }
    }

    @Test
    fun `calibrateDay applies 0_85 multiplier for easier difficulty`() = runTest {
        val log = EnergyLogEntry(
            date = "2025-01-01", activityType = "Meeting",
            energyCost = 3.0, reportedDifficulty = 1,
        )
        coEvery { dao.getLogsByDateOnce("2025-01-01") } returns listOf(log)
        coEvery { dao.getCost("Meeting") } returns null
        repository.calibrateDay("2025-01-01")
        val expected = 3.0 * 0.85
        coVerify { dao.upsertCost(ActivityCostEntry("Meeting", expected, expected, 1)) }
    }

    @Test
    fun `calibrateDay applies 1_2 multiplier for harder difficulty`() = runTest {
        val log = EnergyLogEntry(
            date = "2025-01-01", activityType = "Social",
            energyCost = 2.5, reportedDifficulty = 3,
        )
        coEvery { dao.getLogsByDateOnce("2025-01-01") } returns listOf(log)
        coEvery { dao.getCost("Social") } returns null
        repository.calibrateDay("2025-01-01")
        val expected = 2.5 * 1.2
        coVerify { dao.upsertCost(ActivityCostEntry("Social", expected, expected, 1)) }
    }

    @Test
    fun `calibrateDay updates existing entry with weighted moving average`() = runTest {
        val log = EnergyLogEntry(
            date = "2025-01-01", activityType = "Focus Work",
            energyCost = 2.0, reportedDifficulty = 3,
        )
        val existing = ActivityCostEntry("Focus Work", 2.0, learnedCost = 2.0, sampleCount = 4)
        coEvery { dao.getLogsByDateOnce("2025-01-01") } returns listOf(log)
        coEvery { dao.getCost("Focus Work") } returns existing
        repository.calibrateDay("2025-01-01")
        val actual = 2.0 * 1.2    // 2.4 — harder
        val n = 5
        val expectedLearned = 2.0 + (actual - 2.0) / n  // 2.08
        coVerify { dao.upsertCost(existing.copy(learnedCost = expectedLearned, sampleCount = n)) }
    }

    @Test
    fun `calibrateDay caps sample count at 50`() = runTest {
        val log = EnergyLogEntry(
            date = "2025-01-01", activityType = "Admin",
            energyCost = 1.0, reportedDifficulty = 2,
        )
        coEvery { dao.getLogsByDateOnce("2025-01-01") } returns listOf(log)
        coEvery { dao.getCost("Admin") } returns
            ActivityCostEntry("Admin", 1.0, learnedCost = 1.0, sampleCount = 50)
        repository.calibrateDay("2025-01-01")
        coVerify { dao.upsertCost(match { it.sampleCount == 50 }) }
    }

    // endregion
}
