package org.meow.autistic.data.note

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class NoteRepositoryTest {

    private lateinit var dao: NoteDao
    private lateinit var repository: NoteRepository

    private val note = NoteEntity(
        id = 1,
        content = "Test note",
        createdAt = Instant.ofEpochMilli(1000L),
        updatedAt = Instant.ofEpochMilli(1000L),
    )

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        every { dao.getActiveNotes() } returns flowOf(emptyList())
        every { dao.getDeletedNotes() } returns flowOf(emptyList())
        repository = NoteRepository(dao)
    }

    @Test
    fun `insert stamps updatedAt and pendingFirestoreSync`() = runTest {
        repository.insert(note)
        coVerify { dao.insert(match { it.pendingFirestoreSync && it.content == note.content }) }
    }

    @Test
    fun `update stamps updatedAt and pendingFirestoreSync`() = runTest {
        repository.update(note)
        coVerify { dao.update(match { it.pendingFirestoreSync && it.content == note.content }) }
    }

    @Test
    fun `softDelete delegates to dao with current timestamp`() = runTest {
        repository.softDelete(note.id)
        coVerify { dao.softDelete(note.id, any()) }
    }

    @Test
    fun `restore delegates to dao with current timestamp`() = runTest {
        repository.restore(note.id)
        coVerify { dao.restore(note.id, any()) }
    }

    @Test
    fun `hardDelete delegates to dao`() = runTest {
        repository.hardDelete(note)
        coVerify { dao.delete(note) }
    }
}
