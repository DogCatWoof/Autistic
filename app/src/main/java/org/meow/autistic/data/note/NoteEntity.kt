package org.meow.autistic.data.note

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** A single free-form note stored in Room. */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
