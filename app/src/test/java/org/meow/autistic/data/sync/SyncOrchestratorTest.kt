package org.meow.autistic.data.sync

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.calendar.CalendarSyncService
import org.meow.autistic.data.firestore.FirestoreSyncService
import org.meow.autistic.data.task.GoogleTasksSyncService
import java.time.Instant

class SyncOrchestratorTest {

    private val authManager = mockk<GoogleAuthManager>()
    private val tasksSyncService = mockk<GoogleTasksSyncService>(relaxed = true)
    private val calendarSyncService = mockk<CalendarSyncService>(relaxed = true)
    private val firestoreSyncService = mockk<FirestoreSyncService>(relaxed = true)
    private var firebaseUid: String? = "test-uid"
    private var lastPullAt: Instant? = null
    private var recordedPullAt: Instant? = null
    private lateinit var orchestrator: SyncOrchestrator

    @Before
    fun setUp() {
        every { authManager.isAuthenticated() } returns true
        firebaseUid = "test-uid"
        lastPullAt = null
        recordedPullAt = null
        orchestrator = SyncOrchestrator(
            authManager = authManager,
            tasksSyncService = tasksSyncService,
            calendarSyncService = calendarSyncService,
            firestoreSyncService = firestoreSyncService,
            firebaseUidProvider = { firebaseUid },
            lastFirestorePullAt = { lastPullAt },
            recordFirestorePullAt = { instant -> recordedPullAt = instant },
        )
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
            firestoreSyncService.pushPending("test-uid")
            firestoreSyncService.pullAndMerge("test-uid", null)
        }
    }

    // endregion

    // region failure handling — steps 2-4

    @Test
    fun `sync returns Error when pushPending throws`() = runTest {
        coEvery { tasksSyncService.pushPending() } throws RuntimeException("network error")

        assertEquals(SyncOutcome.Error("network error"), orchestrator.sync())
    }

    @Test
    fun `sync returns Error when tasks pullAndMerge throws`() = runTest {
        coEvery { tasksSyncService.pullAndMerge() } throws RuntimeException("network error")

        assertEquals(SyncOutcome.Error("network error"), orchestrator.sync())
    }

    @Test
    fun `sync returns Error when calendar pullAndMerge throws`() = runTest {
        coEvery { calendarSyncService.pullAndMerge() } throws RuntimeException("network error")

        assertEquals(SyncOutcome.Error("network error"), orchestrator.sync())
    }

    @Test
    fun `sync aborts pipeline on first failure`() = runTest {
        coEvery { tasksSyncService.pullAndMerge() } throws RuntimeException("network error")

        orchestrator.sync()

        coVerify(exactly = 0) { calendarSyncService.pullAndMerge() }
    }

    // endregion

    // region Firestore — step 5-6

    @Test
    fun `sync skips Firestore when no Firebase user`() = runTest {
        firebaseUid = null

        val result = orchestrator.sync()

        assertEquals(SyncOutcome.Success, result)
        coVerify(exactly = 0) { firestoreSyncService.pushPending(any()) }
        coVerify(exactly = 0) { firestoreSyncService.pullAndMerge(any(), any()) }
    }

    @Test
    fun `sync passes last pull timestamp to Firestore pull`() = runTest {
        val since = Instant.parse("2025-01-01T00:00:00Z")
        lastPullAt = since

        orchestrator.sync()

        coVerify { firestoreSyncService.pullAndMerge("test-uid", since) }
    }

    @Test
    fun `sync records pull timestamp after Firestore pull`() = runTest {
        orchestrator.sync()

        assert(recordedPullAt != null)
    }

    @Test
    fun `sync still succeeds when Firestore push throws`() = runTest {
        coEvery { firestoreSyncService.pushPending(any()) } throws RuntimeException("firestore error")

        assertEquals(SyncOutcome.Success, orchestrator.sync())
    }

    @Test
    fun `sync still succeeds when Firestore pull throws`() = runTest {
        coEvery { firestoreSyncService.pullAndMerge(any(), any()) } throws RuntimeException("firestore error")

        assertEquals(SyncOutcome.Success, orchestrator.sync())
    }

    // endregion
}
