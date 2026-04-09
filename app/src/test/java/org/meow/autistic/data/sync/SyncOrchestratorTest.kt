package org.meow.autistic.data.sync

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.calendar.CalendarSyncService
import org.meow.autistic.data.task.GoogleTasksSyncService

class SyncOrchestratorTest {

    private val authManager = mockk<GoogleAuthManager>()
    private val tasksSyncService = mockk<GoogleTasksSyncService>(relaxed = true)
    private val calendarSyncService = mockk<CalendarSyncService>(relaxed = true)
    private val orchestrator = SyncOrchestrator(authManager, tasksSyncService, calendarSyncService)

    @Before
    fun setUp() {
        every { authManager.isAuthenticated() } returns true
    }

    // region authentication gate

    @Test
    fun `sync returns Retry when not authenticated`() = runTest {
        every { authManager.isAuthenticated() } returns false

        assertEquals(SyncOutcome.Retry, orchestrator.sync())
        coVerify(exactly = 0) { tasksSyncService.pushPending() }
    }

    // endregion

    // region happy path

    @Test
    fun `sync returns Success when all steps complete`() = runTest {
        assertEquals(SyncOutcome.Success, orchestrator.sync())
    }

    @Test
    fun `sync runs steps in order when authenticated`() = runTest {
        orchestrator.sync()

        coVerifyOrder {
            tasksSyncService.pushPending()
            tasksSyncService.pullAndMerge()
            calendarSyncService.pullAndMerge()
        }
    }

    // endregion

    // region failure handling

    @Test
    fun `sync returns Retry when pushPending throws`() = runTest {
        coEvery { tasksSyncService.pushPending() } throws RuntimeException("network error")

        assertEquals(SyncOutcome.Retry, orchestrator.sync())
    }

    @Test
    fun `sync returns Retry when tasks pullAndMerge throws`() = runTest {
        coEvery { tasksSyncService.pullAndMerge() } throws RuntimeException("network error")

        assertEquals(SyncOutcome.Retry, orchestrator.sync())
    }

    @Test
    fun `sync returns Retry when calendar pullAndMerge throws`() = runTest {
        coEvery { calendarSyncService.pullAndMerge() } throws RuntimeException("network error")

        assertEquals(SyncOutcome.Retry, orchestrator.sync())
    }

    @Test
    fun `sync aborts pipeline on first failure`() = runTest {
        coEvery { tasksSyncService.pullAndMerge() } throws RuntimeException("network error")

        orchestrator.sync()

        coVerify(exactly = 0) { calendarSyncService.pullAndMerge() }
    }

    // endregion
}
