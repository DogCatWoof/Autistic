package org.meow.autistic.data.calendar

import java.time.Instant
import java.time.temporal.ChronoUnit

private const val FULL_SYNC_DAYS = 60L

/**
 * Orchestrates read-only sync between Google Calendar and the local Room database.
 *
 * Always performs a full 60-day rolling-window sync. Cancelled events are deleted
 * locally; all other events are upserted.
 *
 * @param remoteSource Raw HTTP calls to the Google Calendar API.
 * @param repository Local persistence for calendar events.
 * @param tokenProvider Suspending supplier of a valid OAuth access token.
 */
class CalendarSyncService(
    private val remoteSource: CalendarRemoteSource,
    private val repository: CalendarRepository,
    private val tokenProvider: suspend () -> String,
) {

    /**
     * Pushes pending deletes to Google Calendar, then fetches and merges the last
     * [FULL_SYNC_DAYS] days of events from the remote.
     */
    suspend fun pullAndMerge() {
        val token = tokenProvider()
        pushPendingDeletes(token)
        val timeMin = Instant.now().minus(FULL_SYNC_DAYS, ChronoUnit.DAYS).toEpochMilli()
        val result = remoteSource.fetchEvents(token, timeMin)
        applyResult(result)
    }

    private suspend fun pushPendingDeletes(token: String) {
        val pending = repository.getPendingDeletes()
        for (event in pending) {
            remoteSource.deleteEvent(token, event.googleEventId)
            repository.deleteByIds(listOf(event.googleEventId))
        }
    }

    private suspend fun applyResult(result: CalendarSyncResult) {
        val cancelled = result.events.filter { it.status == "cancelled" }
        val active = result.events.filter { it.status != "cancelled" }

        if (cancelled.isNotEmpty()) {
            repository.deleteByIds(cancelled.map { it.id })
        }
        if (active.isNotEmpty()) {
            val now = Instant.now()
            repository.upsertEvents(active.map { it.toEntity(now) })
        }
    }
}

private fun RemoteEvent.toEntity(now: Instant) = CalendarEventEntity(
    googleEventId = id,
    title = title,
    startAt = Instant.ofEpochMilli(startMs),
    endAt = Instant.ofEpochMilli(endMs),
    isAllDay = isAllDay,
    calendarId = "primary",
    lastSyncedAt = now,
    location = location,
    description = description,
    reminderMinutes = reminderMinutes,
)
