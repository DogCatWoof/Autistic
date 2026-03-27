package org.meow.autistic.data.todo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoEntityTest {

    @Test
    fun `default values are applied correctly`() {
        val entity = TodoEntity(task = "Test", createdAt = 1000L)
        assertEquals(0L, entity.id)
        assertFalse(entity.isCompleted)
        assertNull(entity.dueAt)
        assertEquals("General", entity.category)
        assertFalse(entity.reminderSet)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val original = TodoEntity(id = 1L, task = "Original", createdAt = 1000L, category = "Work")
        val copy = original.copy(isCompleted = true)
        assertEquals(original.id, copy.id)
        assertEquals(original.task, copy.task)
        assertEquals(original.category, copy.category)
        assertEquals(original.createdAt, copy.createdAt)
        assertTrue(copy.isCompleted)
    }

    @Test
    fun `copy can change task`() {
        val original = TodoEntity(id = 1L, task = "Old", createdAt = 1000L)
        val updated = original.copy(task = "New")
        assertEquals("New", updated.task)
        assertEquals(original.id, updated.id)
    }

    @Test
    fun `equality holds for same data`() {
        val a = TodoEntity(id = 1L, task = "Task", createdAt = 1000L)
        val b = TodoEntity(id = 1L, task = "Task", createdAt = 1000L)
        assertEquals(a, b)
    }

    @Test
    fun `inequality when id differs`() {
        val a = TodoEntity(id = 1L, task = "Task", createdAt = 1000L)
        val b = TodoEntity(id = 2L, task = "Task", createdAt = 1000L)
        assertNotEquals(a, b)
    }

    @Test
    fun `inequality when task differs`() {
        val a = TodoEntity(id = 1L, task = "A", createdAt = 1000L)
        val b = TodoEntity(id = 1L, task = "B", createdAt = 1000L)
        assertNotEquals(a, b)
    }

    @Test
    fun `dueAt can be set`() {
        val entity = TodoEntity(task = "Task", createdAt = 1000L, dueAt = 9999L)
        assertEquals(9999L, entity.dueAt)
    }

    @Test
    fun `sync fields default to null and local`() {
        val entity = TodoEntity(task = "Task", createdAt = 1000L)
        assertNull(entity.googleTaskId)
        assertNull(entity.googleTaskListId)
        assertNull(entity.extraPropertiesJson)
        assertNull(entity.lastSyncedAt)
        assertEquals("local", entity.syncStatus)
    }

    @Test
    fun `sync fields can be set`() {
        val entity = TodoEntity(
            task = "Task",
            createdAt = 1000L,
            googleTaskId = "gtask-1",
            googleTaskListId = "list-1",
            extraPropertiesJson = """{"key":"val"}""",
            lastSyncedAt = 5000L,
            syncStatus = "synced",
        )
        assertEquals("gtask-1", entity.googleTaskId)
        assertEquals("list-1", entity.googleTaskListId)
        assertEquals("""{"key":"val"}""", entity.extraPropertiesJson)
        assertEquals(5000L, entity.lastSyncedAt)
        assertEquals("synced", entity.syncStatus)
    }
}
