package org.meow.autistic.data.firestore

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.meow.autistic.data.mood.MoodEntity
import org.meow.autistic.data.note.NoteEntity
import org.meow.autistic.data.task.TaskEntity
import java.time.Instant

class FirestoreDocumentsTest {

    // ── helper ──────────────────────────────────────────────────────────────

    private fun instant(epochSecond: Long) = Instant.ofEpochSecond(epochSecond)

    // ── Instant ↔ Timestamp ─────────────────────────────────────────────────

    @Test
    fun `toFirestoreTimestamp and toInstant are inverse`() {
        val original = instant(1_700_000_000L)
        val roundTrip = original.toFirestoreTimestamp().toInstant()
        assertEquals(original, roundTrip)
    }

    // ── TaskDocument ─────────────────────────────────────────────────────────

    @Test
    fun `TaskEntity toDocument round-trips all fields`() {
        val entity = TaskEntity(
            id = 1L, firestoreId = "fsid1", task = "Buy milk",
            createdAt = instant(1_000L), dueAt = instant(2_000L),
            notes = "Full fat", category = "Shopping", googleTaskId = "gid",
            dailyTaskId = 99L, expectedTimeMinutes = 30, reminderMinutesBefore = 15,
            isImportant = true, completedAt = instant(3_000L),
            lastModifiedAt = instant(4_000L),
        )

        val doc = entity.toDocument()
        val restored = doc.toEntity(firestoreId = "fsid1", localId = 0)

        assertEquals(entity.task, restored.task)
        assertEquals(entity.createdAt, restored.createdAt)
        assertEquals(entity.dueAt, restored.dueAt)
        assertEquals(entity.notes, restored.notes)
        assertEquals(entity.category, restored.category)
        assertEquals(entity.googleTaskId, restored.googleTaskId)
        assertEquals(entity.dailyTaskId, restored.dailyTaskId)
        assertEquals(entity.expectedTimeMinutes, restored.expectedTimeMinutes)
        assertEquals(entity.reminderMinutesBefore, restored.reminderMinutesBefore)
        assertEquals(entity.isImportant, restored.isImportant)
        assertEquals(entity.completedAt, restored.completedAt)
        assertEquals(entity.lastModifiedAt, restored.lastModifiedAt)
        assertEquals("fsid1", restored.firestoreId)
    }

    @Test
    fun `TaskEntity toDocument preserves null optional fields`() {
        val entity = TaskEntity(
            task = "Minimal", createdAt = instant(1_000L), lastModifiedAt = instant(2_000L),
        )
        val doc = entity.toDocument()

        assertNull(doc.dueAt)
        assertNull(doc.notes)
        assertNull(doc.googleTaskId)
        assertNull(doc.dailyTaskId)
        assertNull(doc.expectedTimeMinutes)
        assertNull(doc.reminderMinutesBefore)
        assertNull(doc.completedAt)
    }

    @Test
    fun `TaskDocument toMap contains all keys`() {
        val doc = TaskDocument(
            task = "t", createdAt = Timestamp.now(), dueAt = null, notes = null,
            category = "General", googleTaskId = null, dailyTaskId = null,
            expectedTimeMinutes = null, reminderMinutesBefore = null, isImportant = false,
            completedAt = null, lastModifiedAt = Timestamp.now(),
        )
        val map = doc.toMap()
        val expectedKeys = setOf(
            "task", "createdAt", "dueAt", "notes", "category", "googleTaskId",
            "dailyTaskId", "expectedTimeMinutes", "reminderMinutesBefore",
            "isImportant", "completedAt", "lastModifiedAt",
        )
        assertEquals(expectedKeys, map.keys)
    }

    // ── NoteDocument ─────────────────────────────────────────────────────────

    @Test
    fun `NoteEntity toDocument round-trips all fields`() {
        val entity = NoteEntity(
            id = 5, firestoreId = "nfid", title = "Title", content = "Body",
            createdAt = instant(1_000L), updatedAt = instant(2_000L), isDeleted = false,
        )
        val doc = entity.toDocument()
        val restored = doc.toEntity(firestoreId = "nfid", localId = 0)

        assertEquals(entity.title, restored.title)
        assertEquals(entity.content, restored.content)
        assertEquals(entity.createdAt, restored.createdAt)
        assertEquals(entity.updatedAt, restored.updatedAt)
        assertEquals(entity.isDeleted, restored.isDeleted)
        assertEquals("nfid", restored.firestoreId)
    }

    @Test
    fun `NoteDocument uses updatedAt as lastModifiedAt`() {
        val updatedAt = instant(9_999L)
        val entity = NoteEntity(
            content = "c", createdAt = instant(1_000L), updatedAt = updatedAt,
        )
        val doc = entity.toDocument()
        assertEquals(updatedAt, doc.lastModifiedAt.toInstant())
    }

    // ── MoodDocument ─────────────────────────────────────────────────────────

    @Test
    fun `MoodEntity toDocument round-trips all fields`() {
        val entity = MoodEntity(
            id = 3, firestoreId = "mfid", level = 7, emoji = "😊",
            activity = "Running", notes = "Felt great", createdAt = instant(1_000L),
            isDeleted = false, lastModifiedAt = instant(2_000L),
        )
        val doc = entity.toDocument()
        val restored = doc.toEntity(firestoreId = "mfid", localId = 0)

        assertEquals(entity.level, restored.level)
        assertEquals(entity.emoji, restored.emoji)
        assertEquals(entity.activity, restored.activity)
        assertEquals(entity.notes, restored.notes)
        assertEquals(entity.createdAt, restored.createdAt)
        assertEquals(entity.isDeleted, restored.isDeleted)
        assertEquals(entity.lastModifiedAt, restored.lastModifiedAt)
        assertEquals("mfid", restored.firestoreId)
    }
}
