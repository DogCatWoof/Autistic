package org.meow.autistic.data.conversation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.toneDataStore: DataStore<Preferences> by preferencesDataStore("tone_prefs")
private val TONE_KEY = stringPreferencesKey("tone")

const val TONE_NEUTRAL = "neutral"
const val TONE_POLITE = "polite"
const val TONE_DIRECT = "direct"
val TONES = listOf(TONE_NEUTRAL, TONE_POLITE, TONE_DIRECT)

/** Persists the user's preferred response tone across sessions. */
object TonePreferencesStore {

    fun getToneFlow(context: Context): Flow<String> =
        context.toneDataStore.data.map { it[TONE_KEY] ?: TONE_NEUTRAL }

    suspend fun setTone(context: Context, tone: String) {
        context.toneDataStore.edit { it[TONE_KEY] = tone }
    }
}
