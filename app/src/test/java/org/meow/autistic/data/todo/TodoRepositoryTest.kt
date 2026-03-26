package org.meow.autistic.data.todo

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TodoRepositoryTest {

    private lateinit var dao: TodoDao
    private lateinit var repository: TodoRepository

    private val todo = TodoEntity(id = 1L, task = "Test task", createdAt = 1000L)

    @Before
    fun setUp() {
        dao = mockk()
        repository = TodoRepository(dao)
    }

    @Test
    fun `allTodos exposes flow from dao`() = runTest {
        every { dao.getAllTodos() } returns flowOf(listOf(todo))
        val result = repository.allTodos.first()
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
}
