package org.meow.autistic

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Reports errors to the root UI for display as a Snackbar. */
object GlobalErrorHandler {
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    fun report(message: String) {
        _errors.tryEmit(message)
    }

    fun report(throwable: Throwable) {
        _errors.tryEmit(throwable.message ?: throwable::class.simpleName ?: "Unknown error")
    }
}
