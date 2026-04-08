package org.meow.autistic.data.calendar

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Local mirror of a Google Calendar event.
 *
 * Normally populated exclusively by [CalendarSyncService] during a pull.
 * [isHidden] is set locally when the user "completes" an event (hidden from list, not deleted).
 * [syncStatus] tracks write-back intent: "synced" or "pending_delete".
 */
@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey
    val googleEventId: String,
    val title: String,
    val startAt: Instant,
    val endAt: Instant,
    val isAllDay: Boolean,
    val calendarId: String,
    val lastSyncedAt: Instant,
    val isHidden: Boolean = false,
    val syncStatus: String = "synced",
)
