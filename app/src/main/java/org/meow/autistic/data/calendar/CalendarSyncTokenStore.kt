package org.meow.autistic.data.calendar

import android.content.Context

private const val PREFS_NAME = "calendar_sync"
private const val KEY_SYNC_TOKEN = "sync_token"

/**
 * Persists the Google Calendar incremental-sync token across app restarts.
 * The token is not sensitive, so plain [android.content.SharedPreferences] is used.
 */
class CalendarSyncTokenStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns the stored sync token, or null if no sync has been completed yet. */
    fun getSyncToken(): String? = prefs.getString(KEY_SYNC_TOKEN, null)

    /** Persists [token] for use in the next incremental sync. */
    fun saveSyncToken(token: String) = prefs.edit().putString(KEY_SYNC_TOKEN, token).apply()

    /** Clears the stored token, forcing a full re-sync on the next call. */
    fun clearSyncToken() = prefs.edit().remove(KEY_SYNC_TOKEN).apply()
}
