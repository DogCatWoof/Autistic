package org.meow.autistic.data.energy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.meow.autistic.data.task.TaskDatabase

@RunWith(AndroidJUnit4::class)
class EnergyDaoTest {

    private lateinit var db: TaskDatabase
    private lateinit var dao: EnergyDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.energyDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // region profile

    @Test
    fun getProfile_returnsNullWhenEmpty() = runTest {
        assertNull(dao.getProfile())
    }

    @Test
    fun upsertProfile_insertsAndRetrieves() = runTest {
        dao.upsertProfile(EnergyProfileEntity(dailyCapacity = 8, mode = "Conservative"))
        val result = dao.getProfile()
        assertNotNull(result)
        assertEquals(8, result?.dailyCapacity)
        assertEquals("Conservative", result?.mode)
    }

    @Test
    fun upsertProfile_updatesExistingRecord() = runTest {
        dao.upsertProfile(EnergyProfileEntity(dailyCapacity = 10, mode = "Learning"))
        dao.upsertProfile(EnergyProfileEntity(dailyCapacity = 6, mode = "Protection"))
        val result = dao.getProfile()
        assertEquals(6, result?.dailyCapacity)
        assertEquals("Protection", result?.mode)
    }

    // endregion

    // region energy logs

    @Test
    fun getLogsByDate_returnsEmptyForUnknownDate() = runTest {
        assertTrue(dao.getLogsByDate("2099-01-01").first().isEmpty())
    }

    @Test
    fun insertLog_andGetByDate_returnsEntry() = runTest {
        dao.insertLog(EnergyLogEntry(date = "2025-01-01", activityType = "Focus Work", energyCost = 2.0))
        val results = dao.getLogsByDate("2025-01-01").first()
        assertEquals(1, results.size)
        assertEquals("Focus Work", results[0].activityType)
        assertEquals(2.0, results[0].energyCost, 0.001)
    }

    @Test
    fun getLogsByDate_isolatesEntriesByDate() = runTest {
        dao.insertLog(EnergyLogEntry(date = "2025-01-01", activityType = "Focus Work", energyCost = 2.0))
        dao.insertLog(EnergyLogEntry(date = "2025-01-02", activityType = "Meeting", energyCost = 3.0))
        val results = dao.getLogsByDate("2025-01-01").first()
        assertEquals(1, results.size)
        assertEquals("Focus Work", results[0].activityType)
    }

    @Test
    fun getLogsByDateOnce_returnsAllEntriesForDate() = runTest {
        dao.insertLog(EnergyLogEntry(date = "2025-01-01", activityType = "Admin", energyCost = 1.0))
        dao.insertLog(EnergyLogEntry(date = "2025-01-01", activityType = "Break", energyCost = -1.0))
        assertEquals(2, dao.getLogsByDateOnce("2025-01-01").size)
    }

    @Test
    fun deleteLog_removesEntry() = runTest {
        val id = dao.insertLog(
            EnergyLogEntry(date = "2025-01-01", activityType = "Social", energyCost = 2.5)
        )
        dao.deleteLog(EnergyLogEntry(id = id, date = "2025-01-01", activityType = "Social", energyCost = 2.5))
        assertTrue(dao.getLogsByDate("2025-01-01").first().isEmpty())
    }

    @Test
    fun insertLog_allowsNegativeCostForRecovery() = runTest {
        dao.insertLog(EnergyLogEntry(date = "2025-01-01", activityType = "Nap", energyCost = -2.0))
        val result = dao.getLogsByDate("2025-01-01").first()
        assertEquals(-2.0, result[0].energyCost, 0.001)
    }

    // endregion

    // region activity cost

    @Test
    fun getCost_returnsNullWhenEmpty() = runTest {
        assertNull(dao.getCost("Focus Work"))
    }

    @Test
    fun upsertCost_insertsAndRetrieves() = runTest {
        dao.upsertCost(ActivityCostEntry("Focus Work", baseCost = 2.0, learnedCost = 1.8, sampleCount = 5))
        val result = dao.getCost("Focus Work")
        assertNotNull(result)
        assertEquals(1.8, result!!.learnedCost, 0.001)
        assertEquals(5, result.sampleCount)
    }

    @Test
    fun upsertCost_updatesExistingEntry() = runTest {
        dao.upsertCost(ActivityCostEntry("Meeting", 3.0, 3.0, 1))
        dao.upsertCost(ActivityCostEntry("Meeting", 3.0, 3.2, 2))
        val result = dao.getCost("Meeting")
        assertEquals(3.2, result!!.learnedCost, 0.001)
        assertEquals(2, result.sampleCount)
    }

    @Test
    fun getCost_keepsEntriesSeparateByActivityType() = runTest {
        dao.upsertCost(ActivityCostEntry("Focus Work", 2.0, 1.8, 3))
        dao.upsertCost(ActivityCostEntry("Social", 2.5, 2.7, 2))
        assertEquals(1.8, dao.getCost("Focus Work")!!.learnedCost, 0.001)
        assertEquals(2.7, dao.getCost("Social")!!.learnedCost, 0.001)
    }

    // endregion

    // region start of day

    @Test
    fun getStartOfDay_returnsNullWhenEmpty() = runTest {
        assertNull(dao.getStartOfDay("2025-01-01"))
    }

    @Test
    fun upsertStartOfDay_insertsAndRetrieves() = runTest {
        dao.upsertStartOfDay(
            StartOfDayEntry("2025-01-01", sleepQuality = 4, stressLevel = 2, physicalState = 5, baselineMultiplier = 1.1)
        )
        val result = dao.getStartOfDay("2025-01-01")
        assertNotNull(result)
        assertEquals(4, result?.sleepQuality)
        assertEquals(1.1, result!!.baselineMultiplier, 0.001)
    }

    @Test
    fun upsertStartOfDay_updatesEntryForSameDate() = runTest {
        dao.upsertStartOfDay(StartOfDayEntry("2025-01-01", 3, 3, 3, 0.9))
        dao.upsertStartOfDay(StartOfDayEntry("2025-01-01", 5, 1, 5, 1.2))
        assertEquals(1.2, dao.getStartOfDay("2025-01-01")!!.baselineMultiplier, 0.001)
    }

    @Test
    fun getStartOfDay_keepsEntriesSeparateByDate() = runTest {
        dao.upsertStartOfDay(StartOfDayEntry("2025-01-01", 3, 3, 3, 0.9))
        dao.upsertStartOfDay(StartOfDayEntry("2025-01-02", 5, 1, 5, 1.2))
        assertEquals(0.9, dao.getStartOfDay("2025-01-01")?.baselineMultiplier ?: 0.0, 0.001)
        assertEquals(1.2, dao.getStartOfDay("2025-01-02")?.baselineMultiplier ?: 0.0, 0.001)
    }

    // endregion
}
