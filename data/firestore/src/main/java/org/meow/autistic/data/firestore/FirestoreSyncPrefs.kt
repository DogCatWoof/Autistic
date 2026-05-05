package org.meow.autistic.data.firestore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.Instant

private val Context.firestoreSyncDataStore: DataStore<Preferences> by preferencesDataStore("firestore_sync")
private val LAST_PULL_KEY = longPreferencesKey("last_firestore_pull_epoch_seconds")

/**
 * Persists the epoch-second timestamp of the last successful Firestore pull.
 * Used by [FirestoreSyncService.pullAndMerge] to request only incremental changes.
 */
object FirestoreSyncPrefs {

    suspend fun getLastPullAt(context: Context): Instant? =
        context.firestoreSyncDataStore.data.first()[LAST_PULL_KEY]
            ?.let { Instant.ofEpochSecond(it) }

    suspend fun setLastPullAt(context: Context, instant: Instant) {
        context.firestoreSyncDataStore.edit { it[LAST_PULL_KEY] = instant.epochSecond }
    }
}
