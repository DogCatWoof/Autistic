package org.meow.autistic.data.calendar

import android.os.SystemClock
import kotlinx.coroutines.flow.Flow
import org.meow.autistic.data.diagnostics.QueryLogger

/**
 * Persistence layer for calendar events.
 * All writes come exclusively from [CalendarSyncService]; the UI only reads.
 * Suspend calls are timed via [QueryLogger]; Flow subscriptions are not timed.
 */
class CalendarRepository(
    private val dao: CalendarDao,
    private val queryLogger: QueryLogger,
) {
    fun getAllEvents(): Flow<List<CalendarEventEntity>> = dao.getAllEvents()

    fun getEventsInRange(fromMs: Long, toMs: Long): Flow<List<CalendarEventEntity>> =
        dao.getEventsInRange(fromMs, toMs)

    suspend fun upsertEvents(events: List<CalendarEventEntity>) =
        timed("CalendarRepository.upsertEvents") { dao.upsertEvents(events) }

    suspend fun deleteByIds(ids: List<String>) =
        timed("CalendarRepository.deleteByIds") { dao.deleteByIds(ids) }

    suspend fun deleteAll() =
        timed("CalendarRepository.deleteAll") { dao.deleteAll() }

    private suspend inline fun <T> timed(label: String, block: suspend () -> T): T {
        val start = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            queryLogger.log(label, SystemClock.elapsedRealtime() - start)
        }
    }
}
