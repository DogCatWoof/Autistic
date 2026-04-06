package org.meow.autistic.ui.screens

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.calendar.CalendarRepository
import org.meow.autistic.data.sync.SyncScheduler
import org.meow.autistic.data.todo.TaskEntity
import org.meow.autistic.data.todo.TaskRepository

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    private val repository = mockk<TaskRepository>(relaxed = true)
    private val calendarRepository = mockk<CalendarRepository>(relaxed = true)
    private val authManager = mockk<GoogleAuthManager>()
    private val syncScheduler = mockk<SyncScheduler>(relaxed = true)
    private val workManager = mockk<androidx.work.WorkManager>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: TaskViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { authManager.isAuthenticated() } returns false
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(emptyList())
        every { repository.allTasks } returns flowOf(emptyList())
        every { calendarRepository.getAllEvents() } returns flowOf(emptyList())
        viewModel = TaskViewModel(repository, calendarRepository, authManager, syncScheduler, workManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region update

    @Test
    fun `update sets syncStatus to pending_push for incomplete task`() = runTest {
        val task = TaskEntity(id = 1L, task = "Task", createdAt = 0L, syncStatus = "synced")
        viewModel.update(task)
        coVerify { repository.update(task.copy(syncStatus = "pending_push")) }
    }

    @Test
    fun `update routes completed task to delete`() = runTest {
        val task = TaskEntity(id = 1L, task = "Task", createdAt = 0L, isCompleted = true, googleTaskId = null)
        viewModel.update(task)
        coVerify { repository.delete(task) }
        coVerify(exactly = 0) { repository.update(any()) }
    }

    @Test
    fun `update routes completed synced task to markPendingDelete`() = runTest {
        val task = TaskEntity(id = 1L, task = "Task", createdAt = 0L, isCompleted = true, googleTaskId = "gid")
        viewModel.update(task)
        coVerify { repository.markPendingDelete(1L) }
        coVerify(exactly = 0) { repository.update(any()) }
    }

    @Test
    fun `update preserves pending_push on already-queued item`() = runTest {
        val task = TaskEntity(id = 1L, task = "Task", createdAt = 0L, syncStatus = "pending_push")
        viewModel.update(task)
        coVerify { repository.update(task.copy(syncStatus = "pending_push")) }
    }

    @Test
    fun `update sets pending_push on local item`() = runTest {
        val task = TaskEntity(id = 1L, task = "Task", createdAt = 0L, syncStatus = "local")
        viewModel.update(task)
        coVerify { repository.update(task.copy(syncStatus = "pending_push")) }
    }

    // endregion

    // region delete

    @Test
    fun `delete with googleTaskId marks as pending_delete`() = runTest {
        val task = TaskEntity(id = 1L, task = "Task", createdAt = 0L, googleTaskId = "gid")
        viewModel.delete(task)
        coVerify { repository.markPendingDelete(1L) }
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `delete without googleTaskId deletes directly`() = runTest {
        val task = TaskEntity(id = 1L, task = "Task", createdAt = 0L, googleTaskId = null)
        viewModel.delete(task)
        coVerify { repository.delete(task) }
        coVerify(exactly = 0) { repository.markPendingDelete(any()) }
    }

    // endregion

    // region groupedItems filtering

    private fun makeViewModel(tasks: List<TaskEntity>): TaskViewModel {
        every { repository.allTasks } returns flowOf(tasks)
        return TaskViewModel(repository, calendarRepository, authManager, syncScheduler, workManager)
    }

    @Test
    fun `groupedItems filters out completed tasks`() = runTest {
        val active = TaskEntity(id = 1L, task = "Active", createdAt = 0L, isCompleted = false)
        val completed = TaskEntity(id = 2L, task = "Done", createdAt = 0L, isCompleted = true)
        val vm = makeViewModel(listOf(active, completed))
        val grouped = vm.groupedItems.first()
        val allItems = grouped.today + grouped.later
        assertTrue(allItems.all { it is TaskListItem.Task && !it.entity.isCompleted })
    }

    @Test
    fun `groupedItems filters out pending_delete tasks`() = runTest {
        val active = TaskEntity(id = 1L, task = "Active", createdAt = 0L)
        val pendingDelete = TaskEntity(id = 2L, task = "Deleted", createdAt = 0L, syncStatus = "pending_delete")
        val vm = makeViewModel(listOf(active, pendingDelete))
        val grouped = vm.groupedItems.first()
        val allItems = grouped.today + grouped.later
        assertTrue(allItems.none { it is TaskListItem.Task && it.entity.syncStatus == "pending_delete" })
    }

    @Test
    fun `groupedItems is empty when all tasks are completed or pending_delete`() = runTest {
        val completed = TaskEntity(id = 1L, task = "Done", createdAt = 0L, isCompleted = true)
        val pendingDelete = TaskEntity(id = 2L, task = "Deleted", createdAt = 0L, syncStatus = "pending_delete")
        val vm = makeViewModel(listOf(completed, pendingDelete))
        val grouped = vm.groupedItems.first()
        assertTrue(grouped.today.isEmpty() && grouped.later.isEmpty())
    }

    // endregion
}
