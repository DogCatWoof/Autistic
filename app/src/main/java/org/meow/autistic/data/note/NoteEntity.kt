package org.meow.autistic.data.note

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** Returns the title if set, otherwise the first non-blank content line, or "(empty)". */
val NoteEntity.displayTitle: String
    get() = title.ifBlank { content.lines().firstOrNull { it.isNotBlank() } ?: "(empty)" }

/** A single free-form note stored in Room. */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isDeleted: Boolean = false,
)
