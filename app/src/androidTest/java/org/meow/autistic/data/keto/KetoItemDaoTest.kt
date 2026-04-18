package org.meow.autistic.data.keto

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.meow.autistic.data.task.TaskDatabase
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class KetoItemDaoTest {

    private lateinit var db: TaskDatabase
    private lateinit var dao: KetoItemDao

    private fun entry(date: String, description: String? = null, totalCarbs: Double = 0.0, fiber: Double = 0.0, sugarAlcohols: Double = 0.0) =
        KetoItemEntry(date = date, loggedAt = Instant.now(), description = description, totalCarbs = totalCarbs, fiber = fiber, sugarAlcohols = sugarAlcohols)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.ketoItemDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getByDate_returnsEmptyInitially() = runTest {
        assertTrue(dao.getByDate("2026-04-18").first().isEmpty())
    }

    @Test
    fun insert_then_getByDate_returnsItem() = runTest {
        dao.insert(entry("2026-04-18", description = "Eggs", totalCarbs = 1.0))
        val items = dao.getByDate("2026-04-18").first()
        assertEquals(1, items.size)
        assertEquals("Eggs", items[0].description)
        assertEquals(1.0, items[0].totalCarbs, 0.001)
    }

    @Test
    fun insert_withoutDescription_hasNullDescription() = runTest {
        dao.insert(entry("2026-04-18"))
        val item = dao.getByDate("2026-04-18").first().first()
        assertNull(item.description)
    }

    @Test
    fun insert_multipleItems_returnedInLoggedAtOrder() = runTest {
        val t1 = Instant.ofEpochSecond(1000)
        val t2 = Instant.ofEpochSecond(2000)
        dao.insert(KetoItemEntry(date = "2026-04-18", loggedAt = t2, description = "Bacon"))
        dao.insert(KetoItemEntry(date = "2026-04-18", loggedAt = t1, description = "Eggs"))
        val items = dao.getByDate("2026-04-18").first()
        assertEquals(2, items.size)
        assertEquals("Eggs", items[0].description)
        assertEquals("Bacon", items[1].description)
    }

    @Test
    fun delete_removesItem() = runTest {
        dao.insert(entry("2026-04-18", totalCarbs = 0.5))
        val item = dao.getByDate("2026-04-18").first().first()
        dao.delete(item)
        assertTrue(dao.getByDate("2026-04-18").first().isEmpty())
    }

    @Test
    fun getByDate_doesNotReturnOtherDates() = runTest {
        dao.insert(entry("2026-04-17"))
        assertTrue(dao.getByDate("2026-04-18").first().isEmpty())
    }

    @Test
    fun deleteOlderThan_removesOldRecords() = runTest {
        dao.insert(entry("2026-04-01"))
        dao.insert(entry("2026-04-10"))
        dao.insert(entry("2026-04-18"))

        dao.deleteOlderThan("2026-04-10")

        assertTrue(dao.getByDate("2026-04-01").first().isEmpty())
        assertEquals(1, dao.getByDate("2026-04-10").first().size)
        assertEquals(1, dao.getByDate("2026-04-18").first().size)
    }

    @Test
    fun netCarbs_subtractsFiberAndSugarAlcohols() {
        val item = entry("2026-04-18", totalCarbs = 20.0, fiber = 5.0, sugarAlcohols = 3.0)
        assertEquals(12.0, item.netCarbs, 0.001)
    }

    @Test
    fun netCarbs_clampedToZeroWhenNegative() {
        val item = entry("2026-04-18", totalCarbs = 2.0, fiber = 10.0)
        assertEquals(0.0, item.netCarbs, 0.001)
    }
}
