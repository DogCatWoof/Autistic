package org.meow.autistic.ui.screens

import org.meow.autistic.data.calendar.CalendarEventEntity
import org.meow.autistic.data.task.TaskEntity
import java.time.LocalDate

sealed class TaskListItem {
    abstract val sortKey: Long
    abstract val itemKey: String

    data class Task(val entity: TaskEntity, override val sortKey: Long) : TaskListItem() {
        override val itemKey: String get() = "task_${entity.id}"
    }

    data class Event(val entity: CalendarEventEntity, override val sortKey: Long) : TaskListItem() {
        override val itemKey: String get() = "event_${entity.googleEventId}"
    }
}

data class GroupedTaskItems(
    val pastDue: List<TaskListItem>,
    val today: List<TaskListItem>,
    val later: List<Triple<String, LocalDate?, List<TaskListItem>>>,
) {
    companion object {
        val EMPTY = GroupedTaskItems(emptyList(), emptyList(), emptyList())
    }
}
