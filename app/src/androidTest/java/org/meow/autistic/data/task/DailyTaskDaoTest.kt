package org.meow.autistic.data.task

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyTaskDaoTest {

    private lateinit var db: TaskDatabase
    private lateinit var dao: DailyTaskDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dailyTaskDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getAll_returnsEmptyInitially() = runTest {
        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun insert_appearsInGetAll() = runTest {
        dao.insert(DailyTaskEntity(title = "Morning run"))
        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("Morning run", all[0].title)
    }

    @Test
    fun getPendingFirestoreSync_returnsOnlyPendingItems() = runTest {
        dao.insert(DailyTaskEntity(title = "Pending", pendingFirestoreSync = true))
        dao.insert(DailyTaskEntity(title = "Synced", pendingFirestoreSync = false))
        val result = dao.getPendingFirestoreSync()
        assertEquals(1, result.size)
        assertEquals("Pending", result[0].title)
    }

    @Test
    fun markFirestoreSynced_clearsFlagAndSetsId() = runTest {
        val id = dao.insert(DailyTaskEntity(title = "Task", pendingFirestoreSync = true))
        dao.markFirestoreSynced(id, "fs-daily-1")
        val result = dao.getAllOnce().first { it.id == id }
        assertEquals("fs-daily-1", result.firestoreId)
        assertFalse(result.pendingFirestoreSync)
    }
}
