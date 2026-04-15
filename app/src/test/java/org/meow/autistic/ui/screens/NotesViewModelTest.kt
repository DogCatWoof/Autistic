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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.meow.autistic.data.note.NoteEntity
import org.meow.autistic.data.note.NoteRepository
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    private val repository = mockk<NoteRepository>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: NotesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getActiveNotes() } returns flowOf(emptyList())
        every { repository.getDeletedNotes() } returns flowOf(emptyList())
        viewModel = NotesViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleShowDeleted changes state and switches source`() = runTest {
        val activeNote = NoteEntity(id = 1, title = "Active", content = "...", createdAt = Instant.now(), updatedAt = Instant.now())
        val deletedNote = NoteEntity(id = 2, title = "Deleted", content = "...", createdAt = Instant.now(), updatedAt = Instant.now(), isDeleted = true)
        
        every { repository.getActiveNotes() } returns flowOf(listOf(activeNote))
        every { repository.getDeletedNotes() } returns flowOf(listOf(deletedNote))

        // Initial state: showDeleted = false
        assertFalse(viewModel.showDeleted.value)
        assertEquals(listOf(activeNote), viewModel.notes.first())

        // Toggle
        viewModel.toggleShowDeleted()
        assertTrue(viewModel.showDeleted.value)
        assertEquals(listOf(deletedNote), viewModel.notes.first())
    }

    @Test
    fun `insert calls repository`() = runTest {
        val note = NoteEntity(title = "New", content = "...", createdAt = Instant.now(), updatedAt = Instant.now())
        viewModel.insert(note)
        coVerify { repository.insert(note) }
    }

    @Test
    fun `softDelete calls repository with id`() = runTest {
        val note = NoteEntity(id = 123, title = "X", content = "...", createdAt = Instant.now(), updatedAt = Instant.now())
        viewModel.softDelete(note)
        coVerify { repository.softDelete(123) }
    }

    @Test
    fun `restore calls repository with id`() = runTest {
        val note = NoteEntity(id = 456, title = "X", content = "...", createdAt = Instant.now(), updatedAt = Instant.now())
        viewModel.restore(note)
        coVerify { repository.restore(456) }
    }

    @Test
    fun `hardDelete calls repository`() = runTest {
        val note = NoteEntity(id = 789, title = "X", content = "...", createdAt = Instant.now(), updatedAt = Instant.now())
        viewModel.hardDelete(note)
        coVerify { repository.hardDelete(note) }
    }
}
