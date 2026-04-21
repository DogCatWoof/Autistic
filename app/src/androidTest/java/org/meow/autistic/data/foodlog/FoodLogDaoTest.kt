package org.meow.autistic.data.foodlog

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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.meow.autistic.data.task.TaskDatabase

@RunWith(AndroidJUnit4::class)
class FoodLogDaoTest {

    private lateinit var db: TaskDatabase
    private lateinit var dao: FoodLogDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.foodLogDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getByDate_returnsNullInitially() = runTest {
        assertNull(dao.getByDate("2026-04-14").first())
    }

    @Test
    fun upsert_then_getByDate_returnsEntry() = runTest {
        dao.upsert(FoodLogEntry(date = "2026-04-14", totalCarbs = 18.0, fiber = 4.0))
        val entry = dao.getByDate("2026-04-14").first()
        assertNotNull(entry)
        assertEquals(18.0, entry!!.totalCarbs, 0.001)
        assertEquals(4.0, entry.fiber, 0.001)
    }

    @Test
    fun upsert_replacesExistingEntry() = runTest {
        dao.upsert(FoodLogEntry(date = "2026-04-14", totalCarbs = 10.0))
        dao.upsert(FoodLogEntry(date = "2026-04-14", totalCarbs = 20.0))
        val entry = dao.getByDate("2026-04-14").first()
        assertEquals(20.0, entry!!.totalCarbs, 0.001)
    }

    @Test
    fun getByDate_doesNotReturnOtherDates() = runTest {
        dao.upsert(FoodLogEntry(date = "2026-04-13", totalCarbs = 10.0))
        assertNull(dao.getByDate("2026-04-14").first())
    }

    @Test
    fun deleteOlderThan_removesOldRecords() = runTest {
        dao.upsert(FoodLogEntry(date = "2026-04-01"))
        dao.upsert(FoodLogEntry(date = "2026-04-10"))
        dao.upsert(FoodLogEntry(date = "2026-04-14"))

        dao.deleteOlderThan("2026-04-10")

        assertNull(dao.getByDate("2026-04-01").first())
        assertNotNull(dao.getByDate("2026-04-10").first())
        assertNotNull(dao.getByDate("2026-04-14").first())
    }

    @Test
    fun netCarbs_subtractsFiberAndSugarAlcohols() {
        val entry = FoodLogEntry(date = "2026-04-14", totalCarbs = 20.0, fiber = 5.0, sugarAlcohols = 3.0)
        assertEquals(12.0, entry.netCarbs, 0.001)
    }

    @Test
    fun netCarbs_clampedToZeroWhenNegative() {
        val entry = FoodLogEntry(date = "2026-04-14", totalCarbs = 5.0, fiber = 10.0)
        assertEquals(0.0, entry.netCarbs, 0.001)
    }

    @Test
    fun proteinCalories_isFourTimesProtein() {
        val entry = FoodLogEntry(date = "2026-04-14", protein = 25.0)
        assertEquals(100.0, entry.proteinCalories, 0.001)
    }

    @Test
    fun fatCalories_isNineTimesFat() {
        val entry = FoodLogEntry(date = "2026-04-14", totalFat = 10.0)
        assertEquals(90.0, entry.fatCalories, 0.001)
    }

    @Test
    fun netCarbCalories_isFourTimesNetCarbs() {
        val entry = FoodLogEntry(date = "2026-04-14", totalCarbs = 20.0, fiber = 5.0, sugarAlcohols = 3.0)
        assertEquals(48.0, entry.netCarbCalories, 0.001)
    }
}
