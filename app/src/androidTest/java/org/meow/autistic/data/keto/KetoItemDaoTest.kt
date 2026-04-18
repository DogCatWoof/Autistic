package org.meow.autistic.data.keto

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.meow.autistic.data.task.TaskDatabase

@RunWith(AndroidJUnit4::class)
class KetoItemDaoTest {

    private lateinit var db: TaskDatabase
    private lateinit var dao: KetoItemDao

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
        val items = dao.getByDate("2026-04-18").first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun insert_then_getByDate_returnsItem() = runTest {
        dao.insert(KetoItemEntry(date = "2026-04-18", name = "Eggs", totalCarbs = 1.0, fiber = 0.0))
        val items = dao.getByDate("2026-04-18").first()
        assertEquals(1, items.size)
        assertEquals("Eggs", items[0].name)
        assertEquals(1.0, items[0].totalCarbs, 0.001)
    }

    @Test
    fun insert_multipleItems_returnedInInsertOrder() = runTest {
        dao.insert(KetoItemEntry(date = "2026-04-18", name = "Eggs"))
        dao.insert(KetoItemEntry(date = "2026-04-18", name = "Bacon"))
        val items = dao.getByDate("2026-04-18").first()
        assertEquals(2, items.size)
        assertEquals("Eggs", items[0].name)
        assertEquals("Bacon", items[1].name)
    }

    @Test
    fun delete_removesItem() = runTest {
        val id = dao.insert(KetoItemEntry(date = "2026-04-18", name = "Cheese", totalCarbs = 0.5))
        val item = dao.getByDate("2026-04-18").first().first()
        dao.delete(item)
        assertTrue(dao.getByDate("2026-04-18").first().isEmpty())
    }

    @Test
    fun getByDate_doesNotReturnOtherDates() = runTest {
        dao.insert(KetoItemEntry(date = "2026-04-17", name = "Avocado"))
        assertTrue(dao.getByDate("2026-04-18").first().isEmpty())
    }

    @Test
    fun deleteOlderThan_removesOldRecords() = runTest {
        dao.insert(KetoItemEntry(date = "2026-04-01", name = "Old item"))
        dao.insert(KetoItemEntry(date = "2026-04-10", name = "Boundary item"))
        dao.insert(KetoItemEntry(date = "2026-04-18", name = "Recent item"))

        dao.deleteOlderThan("2026-04-10")

        assertTrue(dao.getByDate("2026-04-01").first().isEmpty())
        assertEquals(1, dao.getByDate("2026-04-10").first().size)
        assertEquals(1, dao.getByDate("2026-04-18").first().size)
    }

    @Test
    fun netCarbs_subtractsFiberAndSugarAlcohols() {
        val item = KetoItemEntry(
            date = "2026-04-18",
            name = "Test",
            totalCarbs = 20.0,
            fiber = 5.0,
            sugarAlcohols = 3.0,
        )
        assertEquals(12.0, item.netCarbs, 0.001)
    }

    @Test
    fun netCarbs_clampedToZeroWhenNegative() {
        val item = KetoItemEntry(date = "2026-04-18", name = "Test", totalCarbs = 2.0, fiber = 10.0)
        assertEquals(0.0, item.netCarbs, 0.001)
    }
}
