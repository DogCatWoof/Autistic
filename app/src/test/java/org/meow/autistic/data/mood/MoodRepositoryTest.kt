package org.meow.autistic.data.mood

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class MoodRepositoryTest {

    private lateinit var dao: MoodDao
    private lateinit var repository: MoodRepository

    private val mood = MoodEntity(id = 1, level = 5, emoji = "😊", createdAt = Instant.ofEpochMilli(1000L))

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        every { dao.getAll() } returns flowOf(emptyList())
        repository = MoodRepository(dao)
    }

    @Test
    fun `insert stamps pendingFirestoreSync and lastModifiedAt`() = runTest {
        repository.insert(mood)
        coVerify { dao.insert(match { it.pendingFirestoreSync && it.emoji == mood.emoji }) }
    }

    @Test
    fun `delete soft-deletes via upsert with isDeleted flag`() = runTest {
        repository.delete(mood)
        coVerify { dao.upsert(match { it.isDeleted && it.pendingFirestoreSync && it.id == mood.id }) }
    }

    @Test
    fun `delete does not call hard-delete dao method`() = runTest {
        repository.delete(mood)
        coVerify(exactly = 0) { dao.delete(any()) }
    }
}
