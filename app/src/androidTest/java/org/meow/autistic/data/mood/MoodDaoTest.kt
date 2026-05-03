package org.meow.autistic.data.mood

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
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class MoodDaoTest {

    private lateinit var db: TaskDatabase
    private lateinit var dao: MoodDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.moodDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getAll_returnsEmptyListInitially() = runTest {
        val moods = dao.getAll().first()
        assertTrue(moods.isEmpty())
    }

    @Test
    fun insert_appearsInGetAll() = runTest {
        val now = Instant.now()
        val mood = MoodEntity(level = 4, activity = "Coding", notes = "Productive", createdAt = now)
        dao.insert(mood)
        
        val moods = dao.getAll().first()
        assertEquals(1, moods.size)
        assertEquals(4, moods[0].level)
        assertEquals("Coding", moods[0].activity)
        assertEquals(now, moods[0].createdAt)
    }

    @Test
    fun delete_removesItem() = runTest {
        val now = Instant.now()
        val mood = MoodEntity(id = 1, level = 3, createdAt = now)
        dao.insert(mood)
        
        val saved = dao.getAll().first()[0]
        dao.delete(saved)
        
        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun getAll_orderedByCreatedAtDescending() = runTest {
        val base = Instant.now()
        dao.insert(MoodEntity(level = 1, createdAt = base))
        dao.insert(MoodEntity(level = 3, createdAt = base.plusSeconds(300)))
        dao.insert(MoodEntity(level = 2, createdAt = base.plusSeconds(150)))

        val result = dao.getAll().first()
        assertEquals(3, result.size)
        assertEquals(3, result[0].level)
        assertEquals(2, result[1].level)
        assertEquals(1, result[2].level)
    }

    @Test
    fun getPendingFirestoreSync_excludesDeleted() = runTest {
        dao.insert(MoodEntity(level = 1, createdAt = Instant.now(), pendingFirestoreSync = true))
        dao.insert(MoodEntity(level = 2, createdAt = Instant.now(), pendingFirestoreSync = true, isDeleted = true))
        val result = dao.getPendingFirestoreSync()
        assertEquals(1, result.size)
        assertEquals(1, result[0].level)
    }

    @Test
    fun getPendingFirestoreDelete_returnsOnlyDeleted() = runTest {
        dao.insert(MoodEntity(level = 1, createdAt = Instant.now(), pendingFirestoreSync = true))
        dao.insert(MoodEntity(level = 2, createdAt = Instant.now(), pendingFirestoreSync = true, isDeleted = true))
        val result = dao.getPendingFirestoreDelete()
        assertEquals(1, result.size)
        assertEquals(2, result[0].level)
    }

    @Test
    fun markFirestoreSynced_clearsFlagAndSetsId() = runTest {
        dao.insert(MoodEntity(id = 1, level = 3, createdAt = Instant.now(), pendingFirestoreSync = true))
        dao.markFirestoreSynced(1, "fs-mood-1")
        val result = dao.getAll().first().first()
        assertEquals("fs-mood-1", result.firestoreId)
        assertEquals(false, result.pendingFirestoreSync)
    }
}
