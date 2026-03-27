package org.meow.autistic.data.calendar

import kotlinx.coroutines.flow.Flow

/**
 * Persistence layer for calendar events.
 * All writes come exclusively from [CalendarSyncService]; the UI only reads.
 */
class CalendarRepository(private val dao: CalendarDao) {
    fun getAllEvents(): Flow<List<CalendarEventEntity>> = dao.getAllEvents()
    fun getEventsInRange(fromMs: Long, toMs: Long): Flow<List<CalendarEventEntity>> =
        dao.getEventsInRange(fromMs, toMs)
    suspend fun upsertEvents(events: List<CalendarEventEntity>) = dao.upsertEvents(events)
    suspend fun deleteByIds(ids: List<String>) = dao.deleteByIds(ids)
    suspend fun deleteAll() = dao.deleteAll()
}
