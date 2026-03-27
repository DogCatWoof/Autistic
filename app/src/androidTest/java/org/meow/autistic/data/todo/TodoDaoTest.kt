package org.meow.autistic.data.todo

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

@RunWith(AndroidJUnit4::class)
class TodoDaoTest {

    private lateinit var db: TodoDatabase
    private lateinit var dao: TodoDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TodoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.todoDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getAllTodos_returnsEmptyListInitially() = runTest {
        val todos = dao.getAllTodos().first()
        assertTrue(todos.isEmpty())
    }

    @Test
    fun insertTodo_appearsInGetAllTodos() = runTest {
        dao.insertTodo(TodoEntity(task = "Test task", createdAt = 1000L))
        val todos = dao.getAllTodos().first()
        assertEquals(1, todos.size)
        assertEquals("Test task", todos[0].task)
    }

    @Test
    fun insertTodo_returnsGeneratedId() = runTest {
        val id = dao.insertTodo(TodoEntity(task = "Task", createdAt = 1000L))
        assertTrue(id > 0)
    }

    @Test
    fun getAllTodos_orderedByCreatedAtDescending() = runTest {
        dao.insertTodo(TodoEntity(task = "First", createdAt = 1000L))
        dao.insertTodo(TodoEntity(task = "Third", createdAt = 3000L))
        dao.insertTodo(TodoEntity(task = "Second", createdAt = 2000L))
        val todos = dao.getAllTodos().first()
        assertEquals("Third", todos[0].task)
        assertEquals("Second", todos[1].task)
        assertEquals("First", todos[2].task)
    }

    @Test
    fun updateTodo_persistsChanges() = runTest {
        dao.insertTodo(TodoEntity(task = "Original", createdAt = 1000L))
        val inserted = dao.getAllTodos().first()[0]
        dao.updateTodo(inserted.copy(task = "Updated", isCompleted = true))
        val updated = dao.getAllTodos().first()[0]
        assertEquals("Updated", updated.task)
        assertTrue(updated.isCompleted)
    }

    @Test
    fun deleteTodo_removesItem() = runTest {
        dao.insertTodo(TodoEntity(task = "To delete", createdAt = 1000L))
        val inserted = dao.getAllTodos().first()[0]
        dao.deleteTodo(inserted)
        assertTrue(dao.getAllTodos().first().isEmpty())
    }

    @Test
    fun deleteTodo_onlyRemovesTargetItem() = runTest {
        dao.insertTodo(TodoEntity(task = "Keep", createdAt = 2000L))
        dao.insertTodo(TodoEntity(task = "Delete", createdAt = 1000L))
        val toDelete = dao.getAllTodos().first().first { it.task == "Delete" }
        dao.deleteTodo(toDelete)
        val remaining = dao.getAllTodos().first()
        assertEquals(1, remaining.size)
        assertEquals("Keep", remaining[0].task)
    }

    @Test
    fun getPendingPush_returnsOnlyPendingPushItems() = runTest {
        dao.insertTodo(TodoEntity(task = "Push me", createdAt = 1000L, syncStatus = "pending_push"))
        dao.insertTodo(TodoEntity(task = "Synced", createdAt = 2000L, syncStatus = "synced"))
        val result = dao.getPendingPush()
        assertEquals(1, result.size)
        assertEquals("Push me", result[0].task)
    }

    @Test
    fun getPendingDelete_returnsOnlyPendingDeleteItems() = runTest {
        dao.insertTodo(TodoEntity(task = "Delete me", createdAt = 1000L, syncStatus = "pending_delete"))
        dao.insertTodo(TodoEntity(task = "Keep me", createdAt = 2000L, syncStatus = "local"))
        val result = dao.getPendingDelete()
        assertEquals(1, result.size)
        assertEquals("Delete me", result[0].task)
    }

    @Test
    fun getByGoogleTaskId_returnsMatchingEntity() = runTest {
        dao.insertTodo(TodoEntity(task = "Task", createdAt = 1000L, googleTaskId = "gid-1"))
        val result = dao.getByGoogleTaskId("gid-1")
        assertEquals("Task", result?.task)
    }

    @Test
    fun getByGoogleTaskId_returnsNullWhenNotFound() = runTest {
        val result = dao.getByGoogleTaskId("nonexistent")
        assertEquals(null, result)
    }

    @Test
    fun updateSyncStatus_changesStatusOnly() = runTest {
        val id = dao.insertTodo(TodoEntity(task = "Task", createdAt = 1000L, syncStatus = "local"))
        dao.updateSyncStatus(id, "pending_push")
        val result = dao.getAllTodos().first().first()
        assertEquals("pending_push", result.syncStatus)
        assertEquals("Task", result.task)
    }

    @Test
    fun markSynced_updatesGoogleTaskIdAndLastSyncedAt() = runTest {
        val id = dao.insertTodo(TodoEntity(task = "Task", createdAt = 1000L, syncStatus = "pending_push"))
        dao.markSynced(id, "gid-1", 9999L)
        val result = dao.getAllTodos().first().first()
        assertEquals("synced", result.syncStatus)
        assertEquals("gid-1", result.googleTaskId)
        assertEquals(9999L, result.lastSyncedAt)
    }

    @Test
    fun deleteByGoogleTaskIds_removesMatchingItems() = runTest {
        dao.insertTodo(TodoEntity(task = "Remove", createdAt = 1000L, googleTaskId = "gid-1"))
        dao.insertTodo(TodoEntity(task = "Keep", createdAt = 2000L, googleTaskId = "gid-2"))
        dao.deleteByGoogleTaskIds(listOf("gid-1"))
        val remaining = dao.getAllTodos().first()
        assertEquals(1, remaining.size)
        assertEquals("Keep", remaining[0].task)
    }
}
