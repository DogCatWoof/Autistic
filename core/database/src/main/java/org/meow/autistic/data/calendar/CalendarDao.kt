package org.meow.autistic.data.calendar

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Persistence operations for [CalendarEventEntity]. */
@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_events WHERE isHidden = 0 AND syncStatus != 'pending_delete' ORDER BY startAt ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE startAt >= :from AND endAt <= :to ORDER BY startAt ASC")
    fun getEventsInRange(from: Instant, to: Instant): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<CalendarEventEntity>)

    @Query("DELETE FROM calendar_events WHERE googleEventId IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAll()

    @Query("UPDATE calendar_events SET isHidden = 1 WHERE googleEventId = :id")
    suspend fun markHidden(id: String)

    @Query("UPDATE calendar_events SET isHidden = 0 WHERE googleEventId = :id")
    suspend fun markVisible(id: String)

    @Query("UPDATE calendar_events SET syncStatus = 'pending_delete' WHERE googleEventId = :id")
    suspend fun markPendingDelete(id: String)

    @Query("SELECT * FROM calendar_events WHERE syncStatus = 'pending_delete'")
    suspend fun getPendingDeletes(): List<CalendarEventEntity>
}
