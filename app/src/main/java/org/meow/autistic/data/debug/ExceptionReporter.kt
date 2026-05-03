package org.meow.autistic.data.debug

import android.content.Context
import android.util.Log
import org.meow.autistic.GlobalErrorHandler

/**
 * Central exception reporting point.
 * In debug mode shows a Snackbar with the exception message.
 * Always logs to Logcat at ERROR level.
 */
class ExceptionReporter(
    private val context: Context,
    private val settings: DebugSettings,
) {
    fun report(e: Throwable) {
        Log.e(TAG, "Unhandled exception: ${e.javaClass.simpleName}", e)
        val msg = buildString {
            append(e.javaClass.simpleName)
            e.message?.let { append(": $it") }
        }
        GlobalErrorHandler.report(msg)
    }

    private companion object {
        const val TAG = "ExceptionReporter"
    }
}
