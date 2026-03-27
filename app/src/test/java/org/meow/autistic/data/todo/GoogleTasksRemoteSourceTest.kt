package org.meow.autistic.data.todo

import com.google.api.services.tasks.Tasks
import com.google.api.services.tasks.model.Task
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoogleTasksRemoteSourceTest {

    private val mockClient = mockk<Tasks>()
    private val mockOps = mockk<Tasks.TasksOperations>()
    private val source = GoogleTasksRemoteSource { mockClient }

    @Before
    fun setUp() {
        every { mockClient.tasks() } returns mockOps
    }

    // region fetchTasks

    @Test
    fun `fetchTasks returns empty list when no items`() = runTest {
        val mockList = mockk<Tasks.TasksOperations.List>()
        every { mockOps.list("@default") } returns mockList
        every { mockList.setShowDeleted(true) } returns mockList
        every { mockList.setShowHidden(true) } returns mockList
        every { mockList.execute() } returns com.google.api.services.tasks.model.Tasks().apply {
            items = emptyList()
            nextPageToken = null
        }

        val result = source.fetchTasks("token")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `fetchTasks maps tasks correctly`() = runTest {
        val mockList = mockk<Tasks.TasksOperations.List>()
        every { mockOps.list("@default") } returns mockList
        every { mockList.setShowDeleted(true) } returns mockList
        every { mockList.setShowHidden(true) } returns mockList
        every { mockList.execute() } returns com.google.api.services.tasks.model.Tasks().apply {
            items = listOf(
                Task().apply {
                    id = "task1"
                    title = "Buy milk"
                    notes = "2% please"
                    status = "needsAction"
                    due = "2026-04-01T00:00:00.000Z"
                    deleted = false
                },
            )
            nextPageToken = null
        }

        val result = source.fetchTasks("token")

        assertEquals(1, result.size)
        with(result[0]) {
            assertEquals("task1", id)
            assertEquals("Buy milk", title)
            assertEquals("2% please", notes)
            assertEquals("needsAction", status)
            assertEquals("2026-04-01T00:00:00.000Z", due)
            assertFalse(deleted)
        }
    }

    @Test
    fun `fetchTasks follows pagination`() = runTest {
        val mockList = mockk<Tasks.TasksOperations.List>()
        every { mockOps.list("@default") } returns mockList
        every { mockList.setShowDeleted(true) } returns mockList
        every { mockList.setShowHidden(true) } returns mockList
        every { mockList.setPageToken("page2") } returns mockList

        val page1Task = Task().apply { id = "t1"; title = "Page 1"; status = "needsAction"; deleted = false }
        val page2Task = Task().apply { id = "t2"; title = "Page 2"; status = "needsAction"; deleted = false }

        every { mockList.execute() } returnsMany listOf(
            com.google.api.services.tasks.model.Tasks().apply {
                items = listOf(page1Task)
                nextPageToken = "page2"
            },
            com.google.api.services.tasks.model.Tasks().apply {
                items = listOf(page2Task)
                nextPageToken = null
            },
        )

        val result = source.fetchTasks("token")

        assertEquals(2, result.size)
        assertEquals("t1", result[0].id)
        assertEquals("t2", result[1].id)
    }

    @Test
    fun `fetchTasks includes deleted tasks`() = runTest {
        val mockList = mockk<Tasks.TasksOperations.List>()
        every { mockOps.list("@default") } returns mockList
        every { mockList.setShowDeleted(true) } returns mockList
        every { mockList.setShowHidden(true) } returns mockList
        every { mockList.execute() } returns com.google.api.services.tasks.model.Tasks().apply {
            items = listOf(
                Task().apply { id = "del1"; title = "Deleted"; status = "needsAction"; deleted = true },
            )
            nextPageToken = null
        }

        val result = source.fetchTasks("token")

        assertTrue(result[0].deleted)
    }

    // endregion

    // region createTask

    @Test
    fun `createTask inserts and returns created task`() = runTest {
        val mockInsert = mockk<Tasks.TasksOperations.Insert>()
        every { mockOps.insert("@default", any()) } returns mockInsert
        every { mockInsert.execute() } returns Task().apply {
            id = "new1"
            title = "New task"
            status = "needsAction"
            deleted = false
        }

        val result = source.createTask("token", RemoteTask(
            id = null, title = "New task", notes = null,
            status = "needsAction", due = null, completed = null, deleted = false,
        ))

        assertEquals("new1", result.id)
        assertEquals("New task", result.title)
    }

    // endregion

    // region updateTask

    @Test
    fun `updateTask sends update and returns updated task`() = runTest {
        val mockUpdate = mockk<Tasks.TasksOperations.Update>()
        every { mockOps.update("@default", "task1", any()) } returns mockUpdate
        every { mockUpdate.execute() } returns Task().apply {
            id = "task1"
            title = "Updated title"
            status = "completed"
            deleted = false
        }

        val result = source.updateTask("token", RemoteTask(
            id = "task1", title = "Updated title", notes = null,
            status = "completed", due = null, completed = null, deleted = false,
        ))

        assertEquals("task1", result.id)
        assertEquals("completed", result.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateTask throws when id is null`() = runTest {
        source.updateTask("token", RemoteTask(
            id = null, title = "No ID", notes = null,
            status = "needsAction", due = null, completed = null, deleted = false,
        ))
    }

    // endregion

    @Test
    fun `fetchTasks returns empty list when response items is null`() = runTest {
        val mockList = mockk<Tasks.TasksOperations.List>()
        every { mockOps.list("@default") } returns mockList
        every { mockList.setShowDeleted(true) } returns mockList
        every { mockList.setShowHidden(true) } returns mockList
        every { mockList.execute() } returns com.google.api.services.tasks.model.Tasks().apply {
            items = null
            nextPageToken = null
        }
        assertTrue(source.fetchTasks("token").isEmpty())
    }

    @Test(expected = IOException::class)
    fun `fetchTasks propagates IOException from API`() = runTest {
        val mockList = mockk<Tasks.TasksOperations.List>()
        every { mockOps.list("@default") } returns mockList
        every { mockList.setShowDeleted(true) } returns mockList
        every { mockList.setShowHidden(true) } returns mockList
        every { mockList.execute() } throws IOException("Network error")
        source.fetchTasks("token")
    }

    @Test(expected = IOException::class)
    fun `createTask propagates IOException from API`() = runTest {
        val mockInsert = mockk<Tasks.TasksOperations.Insert>()
        every { mockOps.insert("@default", any()) } returns mockInsert
        every { mockInsert.execute() } throws IOException("Network error")
        source.createTask("token", RemoteTask(id = null, title = "Task", notes = null, status = "needsAction", due = null, completed = null, deleted = false))
    }

    @Test(expected = IOException::class)
    fun `updateTask propagates IOException from API`() = runTest {
        val mockUpdate = mockk<Tasks.TasksOperations.Update>()
        every { mockOps.update("@default", "task1", any()) } returns mockUpdate
        every { mockUpdate.execute() } throws IOException("Network error")
        source.updateTask("token", RemoteTask(id = "task1", title = "Task", notes = null, status = "needsAction", due = null, completed = null, deleted = false))
    }

    // region deleteTask

    @Test
    fun `deleteTask calls delete on correct task`() = runTest {
        val mockDelete = mockk<Tasks.TasksOperations.Delete>()
        every { mockOps.delete("@default", "task1") } returns mockDelete
        every { mockDelete.execute() } returns null

        source.deleteTask("token", "task1")

        verify(exactly = 1) { mockOps.delete("@default", "task1") }
    }

    @Test(expected = IOException::class)
    fun `deleteTask propagates IOException from API`() = runTest {
        val mockDelete = mockk<Tasks.TasksOperations.Delete>()
        every { mockOps.delete("@default", "task1") } returns mockDelete
        every { mockDelete.execute() } throws IOException("Network error")
        source.deleteTask("token", "task1")
    }

    // endregion
}
