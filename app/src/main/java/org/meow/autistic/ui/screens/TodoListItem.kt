package org.meow.autistic.ui.screens

import org.meow.autistic.data.calendar.CalendarEventEntity
import org.meow.autistic.data.todo.TodoEntity

sealed class TodoListItem {
    abstract val sortKey: Long
    abstract val itemKey: String

    data class Task(val entity: TodoEntity, override val sortKey: Long) : TodoListItem() {
        override val itemKey: String get() = "task_${entity.id}"
    }

    data class Event(val entity: CalendarEventEntity, override val sortKey: Long) : TodoListItem() {
        override val itemKey: String get() = "event_${entity.googleEventId}"
    }
}

data class GroupedTodoItems(
    val pastDue: List<TodoListItem>,
    val today: List<TodoListItem>,
    val later: List<TodoListItem>,
) {
    companion object {
        val EMPTY = GroupedTodoItems(emptyList(), emptyList(), emptyList())
    }
}
