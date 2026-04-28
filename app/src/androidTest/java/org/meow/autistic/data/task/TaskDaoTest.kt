package org.meow.autistic.data.task

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
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var db: TaskDatabase
    private lateinit var dao: TaskDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.taskDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getAllTasks_returnsEmptyListInitially() = runTest {
        val tasks = dao.getAllTasks().first()
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun insertTask_appearsInGetAllTasks() = runTest {
        dao.insertTask(TaskEntity(task = "Test task", createdAt = Instant.ofEpochMilli(1000L)))
        val tasks = dao.getAllTasks().first()
        assertEquals(1, tasks.size)
        assertEquals("Test task", tasks[0].task)
    }

    @Test
    fun insertTask_returnsGeneratedId() = runTest {
        val id = dao.insertTask(TaskEntity(task = "Task", createdAt = Instant.ofEpochMilli(1000L)))
        assertTrue(id > 0)
    }

    @Test
    fun getAllTasks_orderedByCreatedAtDescending() = runTest {
        dao.insertTask(TaskEntity(task = "First", createdAt = Instant.ofEpochMilli(1000L)))
        dao.insertTask(TaskEntity(task = "Third", createdAt = Instant.ofEpochMilli(3000L)))
        dao.insertTask(TaskEntity(task = "Second", createdAt = Instant.ofEpochMilli(2000L)))
        val tasks = dao.getAllTasks().first()
        assertEquals("Third", tasks[0].task)
        assertEquals("Second", tasks[1].task)
        assertEquals("First", tasks[2].task)
    }

    @Test
    fun updateTask_persistsChanges() = runTest {
        dao.insertTask(TaskEntity(task = "Original", createdAt = Instant.ofEpochMilli(1000L)))
        val inserted = dao.getAllTasks().first()[0]
        dao.updateTask(inserted.copy(task = "Updated", isCompleted = true))
        val updated = dao.getAllTasks().first()[0]
        assertEquals("Updated", updated.task)
        assertTrue(updated.isCompleted)
    }

    @Test
    fun deleteTask_removesItem() = runTest {
        dao.insertTask(TaskEntity(task = "To delete", createdAt = Instant.ofEpochMilli(1000L)))
        val inserted = dao.getAllTasks().first()[0]
        dao.deleteTask(inserted)
        assertTrue(dao.getAllTasks().first().isEmpty())
    }

    @Test
    fun deleteTask_onlyRemovesTargetItem() = runTest {
        dao.insertTask(TaskEntity(task = "Keep", createdAt = Instant.ofEpochMilli(2000L)))
        dao.insertTask(TaskEntity(task = "Delete", createdAt = Instant.ofEpochMilli(1000L)))
        val toDelete = dao.getAllTasks().first().first { it.task == "Delete" }
        dao.deleteTask(toDelete)
        val remaining = dao.getAllTasks().first()
        assertEquals(1, remaining.size)
        assertEquals("Keep", remaining[0].task)
    }

    @Test
    fun getPendingPush_includesPendingPushAndLocalNonDailyTasks() = runTest {
        dao.insertTask(TaskEntity(task = "Push me", createdAt = Instant.ofEpochMilli(1000L), syncStatus = "pending_push"))
        dao.insertTask(TaskEntity(task = "Local regular", createdAt = Instant.ofEpochMilli(2000L), syncStatus = "local"))
        dao.insertTask(TaskEntity(task = "Synced", createdAt = Instant.ofEpochMilli(3000L), syncStatus = "synced"))
        dao.insertTask(TaskEntity(task = "Daily local", createdAt = Instant.ofEpochMilli(4000L), syncStatus = "local", dailyTaskId = 1L))
        val result = dao.getPendingPush()
        assertEquals(2, result.size)
        val names = result.map { it.task }.toSet()
        assertTrue(names.contains("Push me"))
        assertTrue(names.contains("Local regular"))
    }

    @Test
    fun getPendingPush_excludesDailyTasksWithLocalStatus() = runTest {
        dao.insertTask(TaskEntity(task = "Daily", createdAt = Instant.ofEpochMilli(1000L), syncStatus = "local", dailyTaskId = 42L))
        val result = dao.getPendingPush()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getPendingDelete_returnsOnlyPendingDeleteItems() = runTest {
        dao.insertTask(
            TaskEntity(
                task = "Delete me",
                createdAt = Instant.ofEpochMilli(1000L),
                syncStatus = "pending_delete"
            )
        )
        dao.insertTask(
            TaskEntity(
                task = "Keep me",
                createdAt = Instant.ofEpochMilli(2000L),
                syncStatus = "local"
            )
        )
        val result = dao.getPendingDelete()
        assertEquals(1, result.size)
        assertEquals("Delete me", result[0].task)
    }

    @Test
    fun getByGoogleTaskId_returnsMatchingEntity() = runTest {
        dao.insertTask(
            TaskEntity(
                task = "Task",
                createdAt = Instant.ofEpochMilli(1000L),
                googleTaskId = "gid-1"
            )
        )
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
        val id = dao.insertTask(
            TaskEntity(
                task = "Task",
                createdAt = Instant.ofEpochMilli(1000L),
                syncStatus = "local"
            )
        )
        dao.updateSyncStatus(id, "pending_push")
        val result = dao.getAllTasks().first().first()
        assertEquals("pending_push", result.syncStatus)
        assertEquals("Task", result.task)
    }

    @Test
    fun markSynced_updatesGoogleTaskIdAndLastSyncedAt() = runTest {
        val id = dao.insertTask(
            TaskEntity(
                task = "Task",
                createdAt = Instant.ofEpochMilli(1000L),
                syncStatus = "pending_push"
            )
        )
        val syncedAt = Instant.ofEpochMilli(9999L)
        dao.markSynced(id, "gid-1", syncedAt)
        val result = dao.getAllTasks().first().first()
        assertEquals("synced", result.syncStatus)
        assertEquals("gid-1", result.googleTaskId)
        assertEquals(syncedAt, result.lastSyncedAt)
    }

    @Test
    fun deleteByGoogleTaskIds_removesMatchingItems() = runTest {
        dao.insertTask(
            TaskEntity(
                task = "Remove",
                createdAt = Instant.ofEpochMilli(1000L),
                googleTaskId = "gid-1"
            )
        )
        dao.insertTask(
            TaskEntity(
                task = "Keep",
                createdAt = Instant.ofEpochMilli(2000L),
                googleTaskId = "gid-2"
            )
        )
        dao.deleteByGoogleTaskIds(listOf("gid-1"))
        val remaining = dao.getAllTasks().first()
        assertEquals(1, remaining.size)
        assertEquals("Keep", remaining[0].task)
    }

    @Test
    fun deleteStaleCompleted_removesOnlyStaleCompletedTasks() = runTest {
        val cutoff = Instant.ofEpochMilli(5000L)
        dao.insertTask(
            TaskEntity(
                task = "Active",
                createdAt = Instant.ofEpochMilli(1000L),
                isCompleted = false,
            )
        )
        dao.insertTask(
            TaskEntity(
                task = "Stale Done",
                createdAt = Instant.ofEpochMilli(2000L),
                isCompleted = true,
                completedAt = Instant.ofEpochMilli(1000L),
            )
        )
        dao.insertTask(
            TaskEntity(
                task = "Recent Done",
                createdAt = Instant.ofEpochMilli(3000L),
                isCompleted = true,
                completedAt = Instant.ofEpochMilli(9000L),
            )
        )
        dao.deleteStaleCompleted(cutoff)
        val remaining = dao.getAllTasks().first()
        assertEquals(2, remaining.size)
        val names = remaining.map { it.task }.toSet()
        assertTrue(names.contains("Active"))
        assertTrue(names.contains("Recent Done"))
    }

    @Test
    fun getCompletedTasks_returnsOnlyCompletedTasks() = runTest {
        dao.insertTask(TaskEntity(task = "Active", createdAt = Instant.ofEpochMilli(1000L), isCompleted = false))
        dao.insertTask(TaskEntity(task = "Done", createdAt = Instant.ofEpochMilli(2000L), isCompleted = true, completedAt = Instant.now()))
        val completed = dao.getCompletedTasks().first()
        assertEquals(1, completed.size)
        assertEquals("Done", completed[0].task)
    }
}
