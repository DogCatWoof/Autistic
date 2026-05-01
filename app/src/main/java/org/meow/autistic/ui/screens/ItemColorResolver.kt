package org.meow.autistic.ui.screens

import androidx.compose.ui.graphics.Color
import org.meow.autistic.data.calendar.CalendarEventEntity
import org.meow.autistic.data.task.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val AMBER_PASTEL = Color(0xFFFFF8E1)
private val RED_PASTEL = Color(0xFFFFEBEE)
private val ORANGE_PASTEL = Color(0xFFFFF3E0)
private val BLUE_PASTEL = Color(0xFFE3F2FD)

fun resolveItemColor(item: TaskListItem, now: Instant): Color = when (item) {
    is TaskListItem.Task -> resolveTaskColor(item.entity, now)
    is TaskListItem.Event -> resolveEventColor(item.entity, now)
}

private fun resolveTaskColor(task: TaskEntity, now: Instant): Color {
    if (task.isImportant) return AMBER_PASTEL
    val due = task.dueAt
    if (due != null) {
        if (due.isBefore(now)) return RED_PASTEL
        if (due.isBefore(now.plusSeconds(7200))) return ORANGE_PASTEL
        val todayEnd = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        if (due.isBefore(todayEnd)) return BLUE_PASTEL
    }
    return Color.Unspecified
}

private fun resolveEventColor(event: CalendarEventEntity, now: Instant): Color {
    if (event.startAt.isBefore(now)) return RED_PASTEL
    if (event.startAt.isBefore(now.plusSeconds(7200))) return ORANGE_PASTEL
    val todayEnd = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
    if (event.startAt.isBefore(todayEnd)) return BLUE_PASTEL
    return Color.Unspecified
}
