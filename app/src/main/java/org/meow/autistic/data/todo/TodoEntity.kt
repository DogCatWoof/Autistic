package org.meow.autistic.data.todo

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * syncStatus values: "local" | "synced" | "pending_push" | "pending_delete"
 * extraPropertiesJson is a JSON blob embedded in the Google Tasks notes field — never shown in UI.
 */
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val task: String,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val dueAt: Long? = null,
    val category: String = "General",
    val reminderSet: Boolean = false,
    val googleTaskId: String? = null,
    val googleTaskListId: String? = null,
    val extraPropertiesJson: String? = null,
    val lastSyncedAt: Long? = null,
    val syncStatus: String = "local",
)
