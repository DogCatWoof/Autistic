package org.meow.autistic.data.calendar

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Persistence operations for [CalendarEventEntity]. Read-only from a UI perspective. */
@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_events ORDER BY startAt ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE startAt >= :fromMs AND endAt <= :toMs ORDER BY startAt ASC")
    fun getEventsInRange(fromMs: Long, toMs: Long): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<CalendarEventEntity>)

    @Query("DELETE FROM calendar_events WHERE googleEventId IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAll()
}
