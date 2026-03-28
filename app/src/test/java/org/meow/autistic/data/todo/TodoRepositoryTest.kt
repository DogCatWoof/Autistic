package org.meow.autistic.data.todo

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.meow.autistic.data.diagnostics.QueryLogger

class TodoRepositoryTest {

    private lateinit var dao: TodoDao
    private lateinit var repository: TodoRepository

    private val todo = TodoEntity(id = 1L, task = "Test task", createdAt = 1000L)

    @Before
    fun setUp() {
        dao = mockk()
        every { dao.getAllTodos() } returns flowOf(emptyList())
        repository = TodoRepository(dao, QueryLogger())
    }

    @Test
    fun `allTodos exposes flow from dao`() = runTest {
        val testDao = mockk<TodoDao>()
        every { testDao.getAllTodos() } returns flowOf(listOf(todo))
        val result = TodoRepository(testDao, QueryLogger()).allTodos.first()
        assertEquals(listOf(todo), result)
    }

    @Test
    fun `insert delegates to dao`() = runTest {
        coEvery { dao.insertTodo(todo) } returns 1L
        repository.insert(todo)
        coVerify(exactly = 1) { dao.insertTodo(todo) }
    }

    @Test
    fun `update delegates to dao`() = runTest {
        coEvery { dao.updateTodo(todo) } returns 1
        repository.update(todo)
        coVerify(exactly = 1) { dao.updateTodo(todo) }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        coEvery { dao.deleteTodo(todo) } returns 1
        repository.delete(todo)
        coVerify(exactly = 1) { dao.deleteTodo(todo) }
    }

    @Test
    fun `getPendingPush delegates to dao`() = runTest {
        coEvery { dao.getPendingPush() } returns listOf(todo)
        val result = repository.getPendingPush()
        assertEquals(listOf(todo), result)
    }

    @Test
    fun `getPendingDelete delegates to dao`() = runTest {
        coEvery { dao.getPendingDelete() } returns listOf(todo)
        val result = repository.getPendingDelete()
        assertEquals(listOf(todo), result)
    }

    @Test
    fun `getByGoogleTaskId delegates to dao`() = runTest {
        coEvery { dao.getByGoogleTaskId("gid") } returns todo
        val result = repository.getByGoogleTaskId("gid")
        assertEquals(todo, result)
    }

    @Test
    fun `markPendingPush calls updateSyncStatus with pending_push`() = runTest {
        coEvery { dao.updateSyncStatus(1L, "pending_push") } returns 1
        repository.markPendingPush(1L)
        coVerify { dao.updateSyncStatus(1L, "pending_push") }
    }

    @Test
    fun `markPendingDelete calls updateSyncStatus with pending_delete`() = runTest {
        coEvery { dao.updateSyncStatus(1L, "pending_delete") } returns 1
        repository.markPendingDelete(1L)
        coVerify { dao.updateSyncStatus(1L, "pending_delete") }
    }

    @Test
    fun `markSynced delegates to dao`() = runTest {
        coEvery { dao.markSynced(1L, "gid", 9999L) } returns 1
        repository.markSynced(1L, "gid", 9999L)
        coVerify { dao.markSynced(1L, "gid", 9999L) }
    }

    @Test
    fun `upsertFromRemote calls insertTodo`() = runTest {
        coEvery { dao.insertTodo(todo) } returns 1L
        repository.upsertFromRemote(todo)
        coVerify { dao.insertTodo(todo) }
    }

    @Test
    fun `deleteByGoogleTaskIds delegates to dao`() = runTest {
        coEvery { dao.deleteByGoogleTaskIds(listOf("gid")) } returns 1
        repository.deleteByGoogleTaskIds(listOf("gid"))
        coVerify { dao.deleteByGoogleTaskIds(listOf("gid")) }
    }

    @Test(expected = RuntimeException::class)
    fun `insert propagates dao exception`() = runTest {
        coEvery { dao.insertTodo(todo) } throws RuntimeException("DB error")
        repository.insert(todo)
    }

    @Test(expected = RuntimeException::class)
    fun `update propagates dao exception`() = runTest {
        coEvery { dao.updateTodo(todo) } throws RuntimeException("DB error")
        repository.update(todo)
    }

    @Test(expected = RuntimeException::class)
    fun `delete propagates dao exception`() = runTest {
        coEvery { dao.deleteTodo(todo) } throws RuntimeException("DB error")
        repository.delete(todo)
    }

    @Test
    fun `getByGoogleTaskId returns null when not found`() = runTest {
        coEvery { dao.getByGoogleTaskId("unknown") } returns null
        assertNull(repository.getByGoogleTaskId("unknown"))
    }

    @Test
    fun `getPendingPush returns empty list when none pending`() = runTest {
        coEvery { dao.getPendingPush() } returns emptyList()
        assertEquals(emptyList<TodoEntity>(), repository.getPendingPush())
    }

    @Test
    fun `deleteByGoogleTaskIds with empty list delegates to dao`() = runTest {
        coEvery { dao.deleteByGoogleTaskIds(emptyList()) } returns 0
        repository.deleteByGoogleTaskIds(emptyList())
        coVerify { dao.deleteByGoogleTaskIds(emptyList()) }
    }
}
