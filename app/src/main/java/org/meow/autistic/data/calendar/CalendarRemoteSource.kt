package org.meow.autistic.data.calendar

import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val APP_NAME = "Autistic"
private const val PRIMARY_CALENDAR = "primary"

/**
 * A single Google Calendar event as returned by the API.
 *
 * @param id Google-assigned event ID.
 * @param title Event summary (title); empty string if not set.
 * @param startMs Epoch milliseconds for event start.
 * @param endMs Epoch milliseconds for event end.
 * @param isAllDay True when the event spans full days with no time component.
 * @param status "confirmed", "tentative", or "cancelled".
 */
data class RemoteEvent(
    val id: String,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val isAllDay: Boolean,
    val status: String,
)

/**
 * Result of a single sync fetch, accumulating all pages.
 *
 * @param events All events returned across all pages.
 * @param nextSyncToken Token from the final response page; pass to the next incremental sync.
 */
data class CalendarSyncResult(
    val events: List<RemoteEvent>,
    val nextSyncToken: String,
)

/**
 * Raw HTTP client for the Google Calendar API (read-only).
 *
 * All methods run on [Dispatchers.IO] and paginate automatically.
 *
 * @param clientFactory Builds the [Calendar] service for a given token. Override in tests.
 */
class CalendarRemoteSource(
    private val clientFactory: (token: String) -> Calendar = { token ->
        Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpRequestInitializer { request ->
                request.headers.authorization = "Bearer $token"
            },
        ).setApplicationName(APP_NAME).build()
    },
) {

    /**
     * Fetches all events in the primary calendar between [timeMin] and [timeMax] (epoch ms).
     * Recurring events are expanded to individual instances. Deleted events are excluded.
     * Returns events plus a [CalendarSyncResult.nextSyncToken] suitable for the next
     * incremental call to [fetchDeletedEvents].
     */
    suspend fun fetchEvents(token: String, timeMin: Long, timeMax: Long): CalendarSyncResult =
        withContext(Dispatchers.IO) {
            val client = clientFactory(token)
            val result = mutableListOf<RemoteEvent>()
            var pageToken: String? = null
            var syncToken = ""
            do {
                val request = client.events().list(PRIMARY_CALENDAR)
                  .setTimeMin(DateTime(timeMin))
                  .setTimeMax(DateTime(timeMax))
                  .setSingleEvents(true)
                  .setShowDeleted(false)
                if (pageToken != null) request.setPageToken(pageToken)
                val response = request.execute()
                response.items?.forEach { result.add(it.toRemoteEvent()) }
                pageToken = response.nextPageToken
                if (response.nextSyncToken != null) syncToken = response.nextSyncToken
            } while (pageToken != null)
            CalendarSyncResult(result, syncToken)
        }

    /**
     * Fetches all changes (new, updated, and cancelled events) since the last sync.
     *
     * @param syncToken Token received from the previous [fetchEvents] or [fetchDeletedEvents] call.
     * @throws com.google.api.client.googleapis.json.GoogleJsonResponseException with status 410
     *   when [syncToken] has expired; caller should discard the token and do a full sync.
     */
    suspend fun fetchDeletedEvents(token: String, syncToken: String): CalendarSyncResult =
        withContext(Dispatchers.IO) {
            val client = clientFactory(token)
            val result = mutableListOf<RemoteEvent>()
            var pageToken: String? = null
            var nextSync = ""
            do {
                val request = client.events().list(PRIMARY_CALENDAR)
                    .setSyncToken(syncToken)
                    .setShowDeleted(true)
                if (pageToken != null) request.setPageToken(pageToken)
                val response = request.execute()
                response.items?.forEach { result.add(it.toRemoteEvent()) }
                pageToken = response.nextPageToken
                if (response.nextSyncToken != null) nextSync = response.nextSyncToken
            } while (pageToken != null)
            CalendarSyncResult(result, nextSync)
        }
}

private fun com.google.api.services.calendar.model.Event.toRemoteEvent() = RemoteEvent(
    id = id ?: "",
    title = summary ?: "",
    startMs = start.toEpochMs(),
    endMs = end.toEpochMs(),
    isAllDay = start?.date != null,
    status = status ?: "confirmed",
)

private fun EventDateTime?.toEpochMs(): Long = (this?.dateTime ?: this?.date)?.value ?: 0L
