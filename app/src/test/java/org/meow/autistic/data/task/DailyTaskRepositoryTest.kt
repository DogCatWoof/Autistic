package org.meow.autistic.data.task

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.meow.autistic.data.diagnostics.QueryLogger

class DailyTaskRepositoryTest {

    private val dao = mockk<DailyTaskDao>(relaxed = true)
    private val repository = DailyTaskRepository(dao, QueryLogger())

    private val sample =
        DailyTaskEntity(id = 1, title = "Morning run", category = "Health", timeMinutes = 420)

    @Test
    fun `insert stamps pendingFirestoreSync and lastModifiedAt`() = runTest {
        repository.insert(sample)
        coVerify { dao.insert(match { it.pendingFirestoreSync && it.title == sample.title }) }
    }

    @Test
    fun `update stamps pendingFirestoreSync and lastModifiedAt`() = runTest {
        repository.update(sample)
        coVerify { dao.update(match { it.pendingFirestoreSync && it.title == sample.title }) }
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
