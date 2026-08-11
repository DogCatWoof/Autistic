package org.meow.autistic.data.note

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
class NoteDaoTest {

    private lateinit var db: TaskDatabase
    private lateinit var dao: NoteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.noteDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getActiveNotes_returnsEmptyListInitially() = runTest {
        val notes = dao.getActiveNotes().first()
        assertTrue(notes.isEmpty())
    }

    @Test
    fun insertNote_appearsInActiveNotes() = runTest {
        val now = Instant.now()
        val note = NoteEntity(title = "Test", content = "Content", createdAt = now, updatedAt = now)
        dao.insert(note)
        
        val notes = dao.getActiveNotes().first()
        assertEquals(1, notes.size)
        assertEquals("Test", notes[0].title)
        assertEquals(false, notes[0].isDeleted)
    }

    @Test
    fun softDelete_movesNoteToDeletedNotes() = runTest {
        val now = Instant.now()
        dao.insert(NoteEntity(id = 1, title = "To Delete", content = "...", createdAt = now, updatedAt = now))
        
        dao.softDelete(1, now.plusSeconds(10))
        
        val active = dao.getActiveNotes().first()
        val deleted = dao.getDeletedNotes().first()
        
        assertTrue(active.isEmpty())
        assertEquals(1, deleted.size)
        assertEquals("To Delete", deleted[0].title)
        assertTrue(deleted[0].isDeleted)
    }

    @Test
    fun restore_movesNoteBackToActive() = runTest {
        val now = Instant.now()
        dao.insert(NoteEntity(id = 1, title = "Restore Me", content = "...", createdAt = now, updatedAt = now, isDeleted = true))
        
        dao.restore(1, now.plusSeconds(10))
        
        val active = dao.getActiveNotes().first()
        val deleted = dao.getDeletedNotes().first()
        
        assertEquals(1, active.size)
        assertTrue(deleted.isEmpty())
        assertEquals("Restore Me", active[0].title)
        assertEquals(false, active[0].isDeleted)
    }

    @Test
    fun hardDelete_removesFromDatabase() = runTest {
        val now = Instant.now()
        val note = NoteEntity(id = 1, title = "Gone", content = "...", createdAt = now, updatedAt = now)
        dao.insert(note)

        dao.delete(note)

        val active = dao.getActiveNotes().first()
        val deleted = dao.getDeletedNotes().first()
        assertTrue(active.isEmpty())
        assertTrue(deleted.isEmpty())
    }

    @Test
    fun getPendingFirestoreSync_excludesDeleted() = runTest {
        val now = Instant.now()
        dao.insert(NoteEntity(title = "Active", content = "...", createdAt = now, updatedAt = now, pendingFirestoreSync = true))
        dao.insert(NoteEntity(title = "Deleted", content = "...", createdAt = now, updatedAt = now, isDeleted = true, pendingFirestoreSync = true))
        val result = dao.getPendingFirestoreSync()
        assertEquals(1, result.size)
        assertEquals("Active", result[0].title)
    }

    @Test
    fun getPendingFirestoreDelete_returnsOnlyDeleted() = runTest {
        val now = Instant.now()
        dao.insert(NoteEntity(title = "Active", content = "...", createdAt = now, updatedAt = now, pendingFirestoreSync = true))
        dao.insert(NoteEntity(title = "Deleted", content = "...", createdAt = now, updatedAt = now, isDeleted = true, pendingFirestoreSync = true))
        val result = dao.getPendingFirestoreDelete()
        assertEquals(1, result.size)
        assertEquals("Deleted", result[0].title)
    }

    @Test
    fun markFirestoreSynced_clearsFlagAndSetsId() = runTest {
        val now = Instant.now()
        dao.insert(NoteEntity(id = 1, title = "Note", content = "...", createdAt = now, updatedAt = now, pendingFirestoreSync = true))
        dao.markFirestoreSynced(1, "fs-note-1")
        val result = dao.getActiveNotes().first().first()
        assertEquals("fs-note-1", result.firestoreId)
        assertEquals(false, result.pendingFirestoreSync)
    }

    @Test
    fun getActiveNotesByTopic_returnsNotesForTopic() = runTest {
        val now = Instant.now()
        dao.insert(NoteEntity(id = 1, title = "Work", content = "A", createdAt = now, updatedAt = now, topicId = 1))
        dao.insert(NoteEntity(id = 2, title = "Personal", content = "B", createdAt = now, updatedAt = now, topicId = 2))
        dao.insert(NoteEntity(id = 3, title = "Untagged", content = "C", createdAt = now, updatedAt = now, topicId = null))

        val topic1 = dao.getActiveNotesByTopic(1).first()
        assertEquals(1, topic1.size)
        assertEquals("Work", topic1[0].title)

        val untagged = dao.getActiveNotesByTopic(null).first()
        assertEquals(1, untagged.size)
        assertEquals("Untagged", untagged[0].title)
    }

    @Test
    fun searchNotes_findsByTitleAndContent() = runTest {
        val now = Instant.now()
        dao.insert(NoteEntity(id = 1, title = "Groceries", content = "Buy milk and eggs", createdAt = now, updatedAt = now))
        dao.insert(NoteEntity(id = 2, title = "Meeting", content = "Discuss groceries budget", createdAt = now, updatedAt = now))
        dao.insert(NoteEntity(id = 3, title = "Random", content = "Nothing to do", createdAt = now, updatedAt = now))

        val results = dao.searchNotes("groceries").first()
        assertEquals(2, results.size)
    }

    @Test
    fun searchNotesByTopic_filtersByTopicAndQuery() = runTest {
        val now = Instant.now()
        dao.insert(NoteEntity(id = 1, title = "Plan", content = "Team outing", createdAt = now, updatedAt = now, topicId = 1))
        dao.insert(NoteEntity(id = 2, title = "Notes", content = "Team meeting notes", createdAt = now, updatedAt = now, topicId = 2))

        val results = dao.searchNotesByTopic("team", topicId = 1).first()
        assertEquals(1, results.size)
        assertEquals("Plan", results[0].title)
    }
}
