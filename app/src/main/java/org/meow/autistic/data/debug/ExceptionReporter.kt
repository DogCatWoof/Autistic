package org.meow.autistic.data.debug

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

/**
 * Central exception reporting point.
 * In debug mode shows a Toast with the exception message.
 * Always logs to Logcat at ERROR level.
 */
class ExceptionReporter(
    private val context: Context,
    private val settings: DebugSettings,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun report(e: Throwable) {
        Log.e(TAG, "Unhandled exception: ${e.javaClass.simpleName}", e)
        if (settings.isDebugEnabled) {
            val msg = buildString {
                append(e.javaClass.simpleName)
                e.message?.let { append(": $it") }
            }
            mainHandler.post {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private companion object {
        const val TAG = "ExceptionReporter"
    }
}
