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
import org.meow.autistic.data.mood.MoodEntity
import org.meow.autistic.data.mood.MoodRepository
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MoodViewModelTest {

    private val repository = mockk<MoodRepository>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: MoodViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getAll() } returns flowOf(emptyList())
        viewModel = MoodViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `moods state flow reflects repository data`() = runTest {
        val now = Instant.now()
        val mood = MoodEntity(id = 1, level = 5, createdAt = now)
        every { repository.getAll() } returns flowOf(listOf(mood))
        
        // Re-init VM to pick up the new flow
        val vm = MoodViewModel(repository)
        assertEquals(listOf(mood), vm.moods.first())
    }

    @Test
    fun `insert calls repository`() = runTest {
        val mood = MoodEntity(level = 3, createdAt = Instant.now())
        viewModel.insert(mood)
        coVerify { repository.insert(mood) }
    }

    @Test
    fun `delete calls repository`() = runTest {
        val mood = MoodEntity(id = 1, level = 3, createdAt = Instant.now())
        viewModel.delete(mood)
        coVerify { repository.delete(mood) }
    }
}
