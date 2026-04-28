package org.meow.autistic.data.calendar

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class CalendarSyncServiceTest {

    private val remoteSource = mockk<CalendarRemoteSource>()
    private val repository = mockk<CalendarRepository>(relaxed = true)
    private val token = "test-token"
    private val service = CalendarSyncService(remoteSource, repository) { token }

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
        coEvery { remoteSource.fetchEvents(token, any()) } returns
            CalendarSyncResult(emptyList(), "")
    }

    // region full sync

    @Test
    fun `pullAndMerge always calls fetchEvents`() = runTest {
        service.pullAndMerge()

        coVerify { remoteSource.fetchEvents(token, any()) }
    }

    @Test
    fun `pullAndMerge uses timeMin of 60 days ago`() = runTest {
        val timeMinSlot = slot<Long>()
        coEvery { remoteSource.fetchEvents(token, capture(timeMinSlot)) } returns
            CalendarSyncResult(emptyList(), "")

        service.pullAndMerge()

        val expected = Instant.now().minus(60, ChronoUnit.DAYS).toEpochMilli()
        val delta = Math.abs(timeMinSlot.captured - expected)
        assertTrue("timeMin should be ~60 days ago (delta=${delta}ms)", delta < 5_000L)
    }

    // endregion

    // region event processing

    @Test
    fun `pullAndMerge upserts active events`() = runTest {
        coEvery { remoteSource.fetchEvents(token, any()) } returns
            CalendarSyncResult(listOf(activeEvent), "")

        service.pullAndMerge()

        coVerify {
            repository.upsertEvents(match { it.size == 1 && it[0].googleEventId == "event1" })
        }
    }

    @Test
    fun `pullAndMerge deletes cancelled events`() = runTest {
        coEvery { remoteSource.fetchEvents(token, any()) } returns
            CalendarSyncResult(listOf(cancelledEvent), "")

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
        coEvery { remoteSource.fetchEvents(token, any()) } returns
            CalendarSyncResult(listOf(activeEvent), "")

        service.pullAndMerge()

        coVerify(exactly = 0) { repository.deleteByIds(any()) }
    }

    // endregion
}
