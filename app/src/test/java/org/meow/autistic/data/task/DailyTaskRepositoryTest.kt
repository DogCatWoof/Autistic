package org.meow.autistic.data.todo

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.meow.autistic.data.diagnostics.QueryLogger
import org.meow.autistic.data.task.DailyTaskDao
import org.meow.autistic.data.task.DailyTaskEntity
import org.meow.autistic.data.task.DailyTaskRepository

class DailyTaskRepositoryTest {

    private val dao = mockk<DailyTaskDao>(relaxed = true)
    private val repository = DailyTaskRepository(dao, QueryLogger())

    private val sample =
        DailyTaskEntity(id = 1, title = "Morning run", category = "Health", timeMinutes = 420)

    @Test
    fun `insert delegates to dao`() = runTest {
        repository.insert(sample)
        coVerify { dao.insert(sample) }
    }

    @Test
    fun `update delegates to dao`() = runTest {
        repository.update(sample)
        coVerify { dao.update(sample) }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(sample)
        coVerify { dao.delete(sample) }
    }

    @Test
    fun `getAllOnce delegates to dao`() = runTest {
        repository.getAllOnce()
        coVerify { dao.getAllOnce() }
    }
}
