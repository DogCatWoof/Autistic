package org.meow.autistic.data.todo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val task: String,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val dueAt: Long? = null,
    val category: String = "General",
    val reminderSet: Boolean = false
)
