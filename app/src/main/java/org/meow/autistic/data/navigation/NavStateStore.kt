package org.meow.autistic.data.navigation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.navStateDataStore: DataStore<Preferences> by preferencesDataStore("nav_state")
private val LAST_DESTINATION_KEY = stringPreferencesKey("last_destination")

/**
 * Persists the last active navigation destination so the app returns to the same
 * screen after a full restart (not just rotation, which [rememberSaveable] handles).
 */
object NavStateStore {

    fun getDestinationFlow(context: Context): Flow<String?> =
        context.navStateDataStore.data.map { it[LAST_DESTINATION_KEY] }

    suspend fun saveDestination(context: Context, destination: String) {
        context.navStateDataStore.edit { it[LAST_DESTINATION_KEY] = destination }
    }

    suspend fun clear(context: Context) {
        context.navStateDataStore.edit { it.clear() }
    }
}
