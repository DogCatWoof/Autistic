package org.meow.autistic.data.diagnostics

/**
 * A single timed database query record.
 *
 * @param label Human-readable identifier, e.g. "TodoRepository.insert".
 * @param durationMs Wall-clock duration of the query in milliseconds.
 * @param timestamp [System.currentTimeMillis] at the time the query completed.
 */
data class QueryEntry(
    val label: String,
    val durationMs: Long,
    val timestamp: Long,
)
