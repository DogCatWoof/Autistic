package org.meow.autistic.data.debug

import android.content.Context
import androidx.core.content.edit

/** Persists the debug-mode toggle across app restarts. */
class DebugSettings(context: Context) {
    private val prefs = context.getSharedPreferences("debug_settings", Context.MODE_PRIVATE)

    var isDebugEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEBUG, true)
        set(value) { prefs.edit { putBoolean(KEY_DEBUG, value) } }

    private companion object {
        const val KEY_DEBUG = "debug_enabled"
    }
}
