package org.meow.autistic.data.calendar

import android.util.Log
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
private const val TAG = "CalendarRemoteSource"

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
 */
data class CalendarSyncResult(
    val events: List<RemoteEvent>,
)

/**
 * Raw HTTP client for the Google Calendar API.
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
     * Fetches all events in the primary calendar from [timeMin] onward (epoch ms).
     * Recurring events are expanded to individual instances. Deleted events are excluded.
     */
    suspend fun fetchEvents(token: String, timeMin: Long): CalendarSyncResult =
        withContext(Dispatchers.IO) {
            val client = clientFactory(token)
            val result = mutableListOf<RemoteEvent>()
            var pageToken: String? = null
            do {
                val request = client.events().list(PRIMARY_CALENDAR)
                  .setTimeMin(DateTime(timeMin))
                  .setSingleEvents(true)
                  .setShowDeleted(false)
                if (pageToken != null) request.setPageToken(pageToken)
                val url = request.buildHttpRequestUrl().build()
                Log.d(TAG, "fetchEvents → GET $url")
                val response = request.execute()
                Log.d(TAG, "fetchEvents ← ${response.items?.size ?: 0} events, " +
                    "nextPageToken=${response.nextPageToken}")
                response.items?.forEach { event ->
                    Log.d(TAG, "  event id=${event.id} summary=${event.summary} " +
                        "start=${event.start?.dateTime ?: event.start?.date} " +
                        "status=${event.status}")
                    result.add(event.toRemoteEvent())
                }
                pageToken = response.nextPageToken
            } while (pageToken != null)
            CalendarSyncResult(result)
        }

    /**
     * Deletes a single event from the primary calendar.
     */
    suspend fun deleteEvent(token: String, eventId: String) {
        withContext(Dispatchers.IO) {
            val request = clientFactory(token).events().delete(PRIMARY_CALENDAR, eventId)
            val url = request.buildHttpRequestUrl().build()
            Log.d(TAG, "deleteEvent → DELETE $url")
            request.execute()
            Log.d(TAG, "deleteEvent ← 204 No Content (eventId=$eventId)")
        }
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
