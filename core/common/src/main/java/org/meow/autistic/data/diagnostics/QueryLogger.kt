package org.meow.autistic.data.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory log of recent database query timings.
 *
 * Entries are stored newest-first and capped at [MAX_ENTRIES].
 * Thread-safe: uses [MutableStateFlow.update] (CAS loop) for concurrent callers.
 */
class QueryLogger {

    private val _entries = MutableStateFlow<List<QueryEntry>>(emptyList())
    val entries: StateFlow<List<QueryEntry>> = _entries.asStateFlow()

    fun log(label: String, durationMs: Long) {
        if (durationMs < SLOW_QUERY_THRESHOLD_MS) return
        val entry = QueryEntry(label, durationMs, System.currentTimeMillis())
        _entries.update { (listOf(entry) + it).take(MAX_ENTRIES) }
    }

    fun clear() {
        _entries.value = emptyList()
    }

    companion object {
        private const val MAX_ENTRIES = 200
        private const val SLOW_QUERY_THRESHOLD_MS = 10_000L
    }
}
