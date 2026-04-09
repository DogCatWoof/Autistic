package org.meow.autistic.data.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TaskEntityTest {

    @Test
    fun `default values are applied correctly`() {
        val entity = TaskEntity(task = "Test", createdAt = Instant.ofEpochMilli(1000L))
        assertEquals(0L, entity.id)
        assertFalse(entity.isCompleted)
        assertNull(entity.dueAt)
        assertNull(entity.notes)
        assertEquals("General", entity.category)
        assertFalse(entity.reminderSet)
        assertNull(entity.expectedTimeMinutes)
    }

    @Test
    fun `expectedTimeMinutes can be set and copied`() {
        val entity = TaskEntity(
            task = "Task",
            createdAt = Instant.ofEpochMilli(1000L),
            expectedTimeMinutes = 45
        )
        assertEquals(45, entity.expectedTimeMinutes)
        val copy = entity.copy(expectedTimeMinutes = 90)
        assertEquals(90, copy.expectedTimeMinutes)
        assertEquals(entity.task, copy.task)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val original = TaskEntity(
            id = 1L,
            task = "Original",
            createdAt = Instant.ofEpochMilli(1000L),
            category = "Work"
        )
        val copy = original.copy(isCompleted = true)
        assertEquals(original.id, copy.id)
        assertEquals(original.task, copy.task)
        assertEquals(original.category, copy.category)
        assertEquals(original.createdAt, copy.createdAt)
        assertTrue(copy.isCompleted)
    }

    @Test
    fun `copy can change task`() {
        val original = TaskEntity(id = 1L, task = "Old", createdAt = Instant.ofEpochMilli(1000L))
        val updated = original.copy(task = "New")
        assertEquals("New", updated.task)
        assertEquals(original.id, updated.id)
    }

    @Test
    fun `equality holds for same data`() {
        val a = TaskEntity(id = 1L, task = "Task", createdAt = Instant.ofEpochMilli(1000L))
        val b = TaskEntity(id = 1L, task = "Task", createdAt = Instant.ofEpochMilli(1000L))
        assertEquals(a, b)
    }

    @Test
    fun `inequality when id differs`() {
        val a = TaskEntity(id = 1L, task = "Task", createdAt = Instant.ofEpochMilli(1000L))
        val b = TaskEntity(id = 2L, task = "Task", createdAt = Instant.ofEpochMilli(1000L))
        assertNotEquals(a, b)
    }

    @Test
    fun `inequality when task differs`() {
        val a = TaskEntity(id = 1L, task = "A", createdAt = Instant.ofEpochMilli(1000L))
        val b = TaskEntity(id = 1L, task = "B", createdAt = Instant.ofEpochMilli(1000L))
        assertNotEquals(a, b)
    }

    @Test
    fun `dueAt can be set`() {
        val due = Instant.ofEpochMilli(9999L)
        val entity = TaskEntity(task = "Task", createdAt = Instant.ofEpochMilli(1000L), dueAt = due)
        assertEquals(due, entity.dueAt)
    }

    @Test
    fun `sync fields default to null and local`() {
        val entity = TaskEntity(task = "Task", createdAt = Instant.ofEpochMilli(1000L))
        assertNull(entity.notes)
        assertNull(entity.googleTaskId)
        assertNull(entity.googleTaskListId)
        assertNull(entity.extraPropertiesJson)
        assertNull(entity.lastSyncedAt)
        assertEquals("local", entity.syncStatus)
    }

    @Test
    fun `inequality when isCompleted differs`() {
        val a = TaskEntity(
            id = 1L,
            task = "Task",
            createdAt = Instant.ofEpochMilli(1000L),
            isCompleted = false
        )
        val b = TaskEntity(
            id = 1L,
            task = "Task",
            createdAt = Instant.ofEpochMilli(1000L),
            isCompleted = true
        )
        assertNotEquals(a, b)
    }

    @Test
    fun `inequality when createdAt differs`() {
        val a = TaskEntity(id = 1L, task = "Task", createdAt = Instant.ofEpochMilli(1000L))
        val b = TaskEntity(id = 1L, task = "Task", createdAt = Instant.ofEpochMilli(2000L))
        assertNotEquals(a, b)
    }

    @Test
    fun `inequality when syncStatus differs`() {
        val a =
            TaskEntity(task = "Task", createdAt = Instant.ofEpochMilli(1000L), syncStatus = "local")
        val b = TaskEntity(
            task = "Task",
            createdAt = Instant.ofEpochMilli(1000L),
            syncStatus = "synced"
        )
        assertNotEquals(a, b)
    }

    @Test
    fun `sync fields can be set`() {
        val synced = Instant.ofEpochMilli(5000L)
        val entity = TaskEntity(
            task = "Task",
            createdAt = Instant.ofEpochMilli(1000L),
            googleTaskId = "gtask-1",
            googleTaskListId = "list-1",
            extraPropertiesJson = """{"key":"val"}""",
            lastSyncedAt = synced,
            syncStatus = "synced",
        )
        assertEquals("gtask-1", entity.googleTaskId)
        assertEquals("list-1", entity.googleTaskListId)
        assertEquals("""{"key":"val"}""", entity.extraPropertiesJson)
        assertEquals(synced, entity.lastSyncedAt)
        assertEquals("synced", entity.syncStatus)
    }
}
