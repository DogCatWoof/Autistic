package org.meow.autistic.data.keto

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

    @Test
    fun getByDate_returnsNullInitially() = runTest {
        assertNull(dao.getByDate("2026-04-14").first())
    }

    @Test
    fun upsert_then_getByDate_returnsEntry() = runTest {
        dao.upsert(KetoLogEntry(date = "2026-04-14", fat = 50.0, protein = 80.0))
        val entry = dao.getByDate("2026-04-14").first()
        assertNotNull(entry)
        assertEquals(50.0, entry!!.fat, 0.001)
        assertEquals(80.0, entry.protein, 0.001)
    }

    @Test
    fun upsert_replacesExistingEntry() = runTest {
        dao.upsert(KetoLogEntry(date = "2026-04-14", fat = 50.0))
        dao.upsert(KetoLogEntry(date = "2026-04-14", fat = 90.0))
        val entry = dao.getByDate("2026-04-14").first()
        assertEquals(90.0, entry!!.fat, 0.001)
    }

    @Test
    fun getByDate_doesNotReturnOtherDates() = runTest {
        dao.upsert(KetoLogEntry(date = "2026-04-13", fat = 10.0))
        assertNull(dao.getByDate("2026-04-14").first())
    }

    @Test
    fun deleteOlderThan_removesOldRecords() = runTest {
        dao.upsert(KetoLogEntry(date = "2026-04-01"))
        dao.upsert(KetoLogEntry(date = "2026-04-10"))
        dao.upsert(KetoLogEntry(date = "2026-04-14"))

        dao.deleteOlderThan("2026-04-10")

        assertNull(dao.getByDate("2026-04-01").first())
        assertNotNull(dao.getByDate("2026-04-10").first())
        assertNotNull(dao.getByDate("2026-04-14").first())
    }

    @Test
    fun netCarbs_subtractsFiberAndSugarAlcohols() {
        val entry = KetoLogEntry(
            date = "2026-04-14",
            totalCarbs = 20.0,
            fiber = 5.0,
            sugarAlcohols = 3.0,
        )
        assertEquals(12.0, entry.netCarbs, 0.001)
    }

    @Test
    fun netCarbs_clampedToZeroWhenNegative() {
        val entry = KetoLogEntry(date = "2026-04-14", totalCarbs = 5.0, fiber = 10.0)
        assertEquals(0.0, entry.netCarbs, 0.001)
    }
}
