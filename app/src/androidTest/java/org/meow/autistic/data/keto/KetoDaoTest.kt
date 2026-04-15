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
class KetoDaoTest {

    private lateinit var db: TaskDatabase
    private lateinit var dao: KetoDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.ketoDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entry(foodName: String, date: String = "2026-04-14") = KetoLogEntry(
        date = date,
        foodName = foodName,
        fat = 10.0,
        protein = 5.0,
        totalCarbs = 3.0,
        fiber = 1.0,
    )

    @Test
    fun getByDate_returnsEmptyInitially() = runTest {
        assertTrue(dao.getByDate("2026-04-14").first().isEmpty())
    }

    @Test
    fun insert_and_getByDate_returnsEntry() = runTest {
        dao.insert(entry("Eggs"))
        val entries = dao.getByDate("2026-04-14").first()
        assertEquals(1, entries.size)
        assertEquals("Eggs", entries[0].foodName)
    }

    @Test
    fun getByDate_filtersOtherDates() = runTest {
        dao.insert(entry("Today", date = "2026-04-14"))
        dao.insert(entry("Yesterday", date = "2026-04-13"))
        val entries = dao.getByDate("2026-04-14").first()
        assertEquals(1, entries.size)
        assertEquals("Today", entries[0].foodName)
    }

    @Test
    fun getByDate_returnsNewestFirst() = runTest {
        dao.insert(entry("First"))
        dao.insert(entry("Second"))
        dao.insert(entry("Third"))
        val entries = dao.getByDate("2026-04-14").first()
        assertEquals(3, entries.size)
        assertEquals("Third", entries[0].foodName)
        assertEquals("Second", entries[1].foodName)
        assertEquals("First", entries[2].foodName)
    }

    @Test
    fun delete_removesEntry() = runTest {
        val id = dao.insert(entry("Bacon"))
        val inserted = dao.getByDate("2026-04-14").first().first { it.id == id }
        dao.delete(inserted)
        assertTrue(dao.getByDate("2026-04-14").first().none { it.id == id })
    }
}
