package org.meow.autistic.data.calendar

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Read-only mirror of a Google Calendar event.
 *
 * Events are never written back to Google Calendar — this table is populated
 * exclusively by [CalendarSyncService] during a pull.
 */
@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey
    val googleEventId: String,
    val title: String,
    val startAt: Long,
    val endAt: Long,
    val isAllDay: Boolean,
    val calendarId: String,
    val lastSyncedAt: Long,
)
