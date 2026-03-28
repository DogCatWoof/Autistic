package org.meow.autistic.data.navigation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.navPrefsDataStore: DataStore<Preferences> by preferencesDataStore("nav_prefs")
private val ENABLED_ITEMS_KEY = stringSetPreferencesKey("enabled_nav_items")

/**
 * Persists the set of enabled navigation titles (leaf items and sub-items).
 *
 * A [null] value from [getEnabledFlow] means no preference has been saved yet;
 * callers should treat [null] as "all items enabled" and supply [allTitles]
 * to [setEnabled] so the first write initialises from the full set.
 */
object NavPreferencesStore {

    fun getEnabledFlow(context: Context): Flow<Set<String>?> =
        context.navPrefsDataStore.data.map { prefs -> prefs[ENABLED_ITEMS_KEY] }

    suspend fun setEnabled(
        context: Context,
        title: String,
        enabled: Boolean,
        allTitles: Set<String>,
    ) {
        context.navPrefsDataStore.edit { prefs ->
            val current = prefs[ENABLED_ITEMS_KEY] ?: allTitles
            prefs[ENABLED_ITEMS_KEY] = if (enabled) current + title else current - title
        }
    }
}
