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
import org.meow.autistic.data.todo.TodoEntity
import org.meow.autistic.data.todo.TodoRepository

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    private val repository = mockk<TodoRepository>(relaxed = true)
    private val calendarRepository = mockk<CalendarRepository>(relaxed = true)
    private val authManager = mockk<GoogleAuthManager>()
    private val syncScheduler = mockk<SyncScheduler>(relaxed = true)
    private val workManager = mockk<androidx.work.WorkManager>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: TodoViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { authManager.isAuthenticated() } returns false
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(emptyList())
        every { repository.allTodos } returns flowOf(emptyList())
        every { calendarRepository.getAllEvents() } returns flowOf(emptyList())
        viewModel = TodoViewModel(repository, calendarRepository, authManager, syncScheduler, workManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region update

    @Test
    fun `update sets syncStatus to pending_push`() = runTest {
        val todo = TodoEntity(id = 1L, task = "Task", createdAt = 0L, syncStatus = "synced")
        viewModel.update(todo)
        coVerify { repository.update(todo.copy(syncStatus = "pending_push")) }
    }

    @Test
    fun `update preserves pending_push on already-queued item`() = runTest {
        val todo = TodoEntity(id = 1L, task = "Task", createdAt = 0L, syncStatus = "pending_push")
        viewModel.update(todo)
        coVerify { repository.update(todo.copy(syncStatus = "pending_push")) }
    }

    @Test
    fun `update sets pending_push on local item`() = runTest {
        val todo = TodoEntity(id = 1L, task = "Task", createdAt = 0L, syncStatus = "local")
        viewModel.update(todo)
        coVerify { repository.update(todo.copy(syncStatus = "pending_push")) }
    }

    // endregion

    // region delete

    @Test
    fun `delete with googleTaskId marks as pending_delete`() = runTest {
        val todo = TodoEntity(id = 1L, task = "Task", createdAt = 0L, googleTaskId = "gid")
        viewModel.delete(todo)
        coVerify { repository.markPendingDelete(1L) }
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `delete without googleTaskId deletes directly`() = runTest {
        val todo = TodoEntity(id = 1L, task = "Task", createdAt = 0L, googleTaskId = null)
        viewModel.delete(todo)
        coVerify { repository.delete(todo) }
        coVerify(exactly = 0) { repository.markPendingDelete(any()) }
    }

    // endregion

    // region groupedItems filtering

    private fun makeViewModel(todos: List<TodoEntity>): TodoViewModel {
        every { repository.allTodos } returns flowOf(todos)
        return TodoViewModel(repository, calendarRepository, authManager, syncScheduler, workManager)
    }

    @Test
    fun `groupedItems filters out completed todos`() = runTest {
        val active = TodoEntity(id = 1L, task = "Active", createdAt = 0L, isCompleted = false)
        val completed = TodoEntity(id = 2L, task = "Done", createdAt = 0L, isCompleted = true)
        val vm = makeViewModel(listOf(active, completed))
        val grouped = vm.groupedItems.first()
        val allItems = grouped.today + grouped.later
        assertTrue(allItems.all { it is TodoListItem.Task && !it.entity.isCompleted })
    }

    @Test
    fun `groupedItems filters out pending_delete todos`() = runTest {
        val active = TodoEntity(id = 1L, task = "Active", createdAt = 0L)
        val pendingDelete = TodoEntity(id = 2L, task = "Deleted", createdAt = 0L, syncStatus = "pending_delete")
        val vm = makeViewModel(listOf(active, pendingDelete))
        val grouped = vm.groupedItems.first()
        val allItems = grouped.today + grouped.later
        assertTrue(allItems.none { it is TodoListItem.Task && it.entity.syncStatus == "pending_delete" })
    }

    @Test
    fun `groupedItems is empty when all todos are completed or pending_delete`() = runTest {
        val completed = TodoEntity(id = 1L, task = "Done", createdAt = 0L, isCompleted = true)
        val pendingDelete = TodoEntity(id = 2L, task = "Deleted", createdAt = 0L, syncStatus = "pending_delete")
        val vm = makeViewModel(listOf(completed, pendingDelete))
        val grouped = vm.groupedItems.first()
        assertTrue(grouped.today.isEmpty() && grouped.later.isEmpty())
    }

    // endregion
}
