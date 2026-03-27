package org.meow.autistic.data.calendar

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class CalendarSyncServiceTest {

    private val remoteSource = mockk<CalendarRemoteSource>()
    private val repository = mockk<CalendarRepository>(relaxed = true)
    private val syncTokenStore = mockk<CalendarSyncTokenStore>(relaxed = true)
    private val token = "test-token"
    private val service = CalendarSyncService(remoteSource, repository, syncTokenStore) { token }

    private val storedSyncToken = "sync-token-1"
    private val newSyncToken = "sync-token-2"
    private val activeEvent = RemoteEvent(
        id = "event1", title = "Meeting", startMs = 1000L, endMs = 2000L,
        isAllDay = false, status = "confirmed",
    )
    private val cancelledEvent = RemoteEvent(
        id = "event2", title = "", startMs = 0L, endMs = 0L,
        isAllDay = false, status = "cancelled",
    )

    @Before
    fun setUp() {
        coEvery { syncTokenStore.getSyncToken() } returns null
        coEvery { remoteSource.fetchEvents(token, any(), any()) } returns
            CalendarSyncResult(emptyList(), newSyncToken)
    }

    // region sync strategy selection

    @Test
    fun `pullAndMerge does full sync when no syncToken stored`() = runTest {
        service.pullAndMerge()

        coVerify { remoteSource.fetchEvents(token, any(), any()) }
        coVerify(exactly = 0) { remoteSource.fetchDeletedEvents(any(), any()) }
    }

    @Test
    fun `pullAndMerge does incremental sync when syncToken stored`() = runTest {
        coEvery { syncTokenStore.getSyncToken() } returns storedSyncToken
        coEvery { remoteSource.fetchDeletedEvents(token, storedSyncToken) } returns
            CalendarSyncResult(emptyList(), newSyncToken)

        service.pullAndMerge()

        coVerify { remoteSource.fetchDeletedEvents(token, storedSyncToken) }
        coVerify(exactly = 0) { remoteSource.fetchEvents(any(), any(), any()) }
    }

    // endregion

    // region syncToken expiry

    @Test
    fun `pullAndMerge falls back to full sync on 410`() = runTest {
        val error = mockk<GoogleJsonResponseException>()
        every { error.statusCode } returns 410
        coEvery { syncTokenStore.getSyncToken() } returns storedSyncToken
        coEvery { remoteSource.fetchDeletedEvents(token, storedSyncToken) } throws error

        service.pullAndMerge()

        coVerify { syncTokenStore.clearSyncToken() }
        coVerify { remoteSource.fetchEvents(token, any(), any()) }
    }

    @Test
    fun `pullAndMerge rethrows non-410 errors`() = runTest {
        val error = mockk<GoogleJsonResponseException>()
        every { error.statusCode } returns 403
        coEvery { syncTokenStore.getSyncToken() } returns storedSyncToken
        coEvery { remoteSource.fetchDeletedEvents(token, storedSyncToken) } throws error

        var caught: GoogleJsonResponseException? = null
        try {
            service.pullAndMerge()
        } catch (e: GoogleJsonResponseException) {
            caught = e
        }
        assertNotNull(caught)
    }

    // endregion

    // region event processing

    @Test
    fun `pullAndMerge upserts active events`() = runTest {
        coEvery { remoteSource.fetchEvents(token, any(), any()) } returns
            CalendarSyncResult(listOf(activeEvent), newSyncToken)

        service.pullAndMerge()

        coVerify {
            repository.upsertEvents(match { it.size == 1 && it[0].googleEventId == "event1" })
        }
    }

    @Test
    fun `pullAndMerge deletes cancelled events`() = runTest {
        coEvery { remoteSource.fetchEvents(token, any(), any()) } returns
            CalendarSyncResult(listOf(cancelledEvent), newSyncToken)

        service.pullAndMerge()

        coVerify { repository.deleteByIds(listOf("event2")) }
    }

    @Test
    fun `pullAndMerge does not upsert when no active events`() = runTest {
        service.pullAndMerge()

        coVerify(exactly = 0) { repository.upsertEvents(any()) }
    }

    @Test
    fun `pullAndMerge does not delete when no cancelled events`() = runTest {
        coEvery { remoteSource.fetchEvents(token, any(), any()) } returns
            CalendarSyncResult(listOf(activeEvent), newSyncToken)

        service.pullAndMerge()

        coVerify(exactly = 0) { repository.deleteByIds(any()) }
    }

    // endregion

    // region syncToken persistence

    @Test
    fun `pullAndMerge saves new syncToken after full sync`() = runTest {
        service.pullAndMerge()

        coVerify { syncTokenStore.saveSyncToken(newSyncToken) }
    }

    @Test
    fun `pullAndMerge saves new syncToken after incremental sync`() = runTest {
        coEvery { syncTokenStore.getSyncToken() } returns storedSyncToken
        coEvery { remoteSource.fetchDeletedEvents(token, storedSyncToken) } returns
            CalendarSyncResult(emptyList(), newSyncToken)

        service.pullAndMerge()

        coVerify { syncTokenStore.saveSyncToken(newSyncToken) }
    }

    @Test
    fun `pullAndMerge does not save syncToken when response token is empty`() = runTest {
        coEvery { remoteSource.fetchEvents(token, any(), any()) } returns
            CalendarSyncResult(emptyList(), "")

        service.pullAndMerge()

        coVerify(exactly = 0) { syncTokenStore.saveSyncToken(any()) }
    }

    // endregion
}
