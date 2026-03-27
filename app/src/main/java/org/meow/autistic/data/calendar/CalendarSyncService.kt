package org.meow.autistic.data.calendar

import com.google.api.client.googleapis.json.GoogleJsonResponseException

private const val FULL_SYNC_WINDOW_MS = 60L * 24 * 60 * 60 * 1000 // 60 days
private const val HTTP_GONE = 410

/**
 * Orchestrates read-only sync between Google Calendar and the local Room database.
 *
 * Uses an incremental sync when a stored [CalendarSyncTokenStore] token is available.
 * Falls back to a full 60-day rolling-window sync when no token exists or when the
 * token has expired (HTTP 410 — Google invalidates tokens after ~7 days of no sync).
 *
 * @param remoteSource Raw HTTP calls to the Google Calendar API.
 * @param repository Local persistence for calendar events.
 * @param syncTokenStore Persists the Google incremental-sync token across app restarts.
 * @param tokenProvider Suspending supplier of a valid OAuth access token.
 */
class CalendarSyncService(
    private val remoteSource: CalendarRemoteSource,
    private val repository: CalendarRepository,
    private val syncTokenStore: CalendarSyncTokenStore,
    private val tokenProvider: suspend () -> String,
) {

    /**
     * Fetches changes from Google Calendar and merges them into the local database.
     *
     * Cancelled events are deleted locally; all other events are upserted.
     * The new syncToken from the response is persisted for the next incremental call.
     */
    suspend fun pullAndMerge() {
        val token = tokenProvider()
        val storedSyncToken = syncTokenStore.getSyncToken()
        val result = if (storedSyncToken != null) {
            incrementalSync(token, storedSyncToken)
        } else {
            fullSync(token)
        }
        applyResult(result)
    }

    private suspend fun incrementalSync(token: String, syncToken: String): CalendarSyncResult {
        return try {
            remoteSource.fetchDeletedEvents(token, syncToken)
        } catch (e: GoogleJsonResponseException) {
            if (e.statusCode == HTTP_GONE) {
                syncTokenStore.clearSyncToken()
                fullSync(token)
            } else {
                throw e
            }
        }
    }

    private suspend fun fullSync(token: String): CalendarSyncResult {
        val now = System.currentTimeMillis()
        return remoteSource.fetchEvents(token, now, now + FULL_SYNC_WINDOW_MS)
    }

    private suspend fun applyResult(result: CalendarSyncResult) {
        val cancelled = result.events.filter { it.status == "cancelled" }
        val active = result.events.filter { it.status != "cancelled" }

        if (cancelled.isNotEmpty()) {
            repository.deleteByIds(cancelled.map { it.id })
        }
        if (active.isNotEmpty()) {
            val now = System.currentTimeMillis()
            repository.upsertEvents(active.map { it.toEntity(now) })
        }
        if (result.nextSyncToken.isNotEmpty()) {
            syncTokenStore.saveSyncToken(result.nextSyncToken)
        }
    }
}

private fun RemoteEvent.toEntity(now: Long) = CalendarEventEntity(
    googleEventId = id,
    title = title,
    startAt = startMs,
    endAt = endMs,
    isAllDay = isAllDay,
    calendarId = "primary",
    lastSyncedAt = now,
)
