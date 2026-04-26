package org.meow.autistic.data.task

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
import java.time.Instant

class TaskRepositoryTest {

    private lateinit var dao: TaskDao
    private lateinit var repository: TaskRepository

    private val task =
        TaskEntity(id = 1L, task = "Test task", createdAt = Instant.ofEpochMilli(1000L))

    @Before
    fun setUp() {
        dao = mockk()
        every { dao.getAllTasks() } returns flowOf(emptyList())
        every { dao.getCompletedTasks() } returns flowOf(emptyList())
        repository = TaskRepository(dao, QueryLogger())
    }

    @Test
    fun `allTasks exposes flow from dao`() = runTest {
        val testDao = mockk<TaskDao>()
        every { testDao.getAllTasks() } returns flowOf(listOf(task))
        every { testDao.getCompletedTasks() } returns flowOf(emptyList())
        val result = TaskRepository(testDao, QueryLogger()).allTasks.first()
        assertEquals(listOf(task), result)
    }

    @Test
    fun `insert delegates to dao`() = runTest {
        coEvery { dao.insertTask(task) } returns 1L
        repository.insert(task)
        coVerify(exactly = 1) { dao.insertTask(task) }
    }

    @Test
    fun `update delegates to dao`() = runTest {
        coEvery { dao.updateTask(task) } returns 1
        repository.update(task)
        coVerify(exactly = 1) { dao.updateTask(task) }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        coEvery { dao.deleteTask(task) } returns 1
        repository.delete(task)
        coVerify(exactly = 1) { dao.deleteTask(task) }
    }

    @Test
    fun `getPendingPush delegates to dao`() = runTest {
        coEvery { dao.getPendingPush() } returns listOf(task)
        val result = repository.getPendingPush()
        assertEquals(listOf(task), result)
    }

    @Test
    fun `getPendingDelete delegates to dao`() = runTest {
        coEvery { dao.getPendingDelete() } returns listOf(task)
        val result = repository.getPendingDelete()
        assertEquals(listOf(task), result)
    }

    @Test
    fun `getByGoogleTaskId delegates to dao`() = runTest {
        coEvery { dao.getByGoogleTaskId("gid") } returns task
        val result = repository.getByGoogleTaskId("gid")
        assertEquals(task, result)
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
        val ts = Instant.ofEpochMilli(9999L)
        coEvery { dao.markSynced(1L, "gid", ts) } returns 1
        repository.markSynced(1L, "gid", ts)
        coVerify { dao.markSynced(1L, "gid", ts) }
    }

    @Test
    fun `upsertFromRemote calls insertTask`() = runTest {
        coEvery { dao.insertTask(task) } returns 1L
        repository.upsertFromRemote(task)
        coVerify { dao.insertTask(task) }
    }

    @Test
    fun `deleteByGoogleTaskIds delegates to dao`() = runTest {
        coEvery { dao.deleteByGoogleTaskIds(listOf("gid")) } returns 1
        repository.deleteByGoogleTaskIds(listOf("gid"))
        coVerify { dao.deleteByGoogleTaskIds(listOf("gid")) }
    }

    @Test
    fun `deleteStaleCompleted delegates to dao with cutoff`() = runTest {
        val cutoff = java.time.Instant.now()
        coEvery { dao.deleteStaleCompleted(cutoff) } returns 2
        repository.deleteStaleCompleted(cutoff)
        coVerify { dao.deleteStaleCompleted(cutoff) }
    }

    @Test(expected = RuntimeException::class)
    fun `insert propagates dao exception`() = runTest {
        coEvery { dao.insertTask(task) } throws RuntimeException("DB error")
        repository.insert(task)
    }

    @Test(expected = RuntimeException::class)
    fun `update propagates dao exception`() = runTest {
        coEvery { dao.updateTask(task) } throws RuntimeException("DB error")
        repository.update(task)
    }

    @Test(expected = RuntimeException::class)
    fun `delete propagates dao exception`() = runTest {
        coEvery { dao.deleteTask(task) } throws RuntimeException("DB error")
        repository.delete(task)
    }

    @Test
    fun `getByGoogleTaskId returns null when not found`() = runTest {
        coEvery { dao.getByGoogleTaskId("unknown") } returns null
        assertNull(repository.getByGoogleTaskId("unknown"))
    }

    @Test
    fun `getPendingPush returns empty list when none pending`() = runTest {
        coEvery { dao.getPendingPush() } returns emptyList()
        assertEquals(emptyList<TaskEntity>(), repository.getPendingPush())
    }

    @Test
    fun `deleteByGoogleTaskIds with empty list delegates to dao`() = runTest {
        coEvery { dao.deleteByGoogleTaskIds(emptyList()) } returns 0
        repository.deleteByGoogleTaskIds(emptyList())
        coVerify { dao.deleteByGoogleTaskIds(emptyList()) }
    }
}
