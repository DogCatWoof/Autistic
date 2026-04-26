package org.meow.autistic.data.sequence

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A named, ordered checklist of steps that a user can run repeatedly. */
@Entity(tableName = "sequences")
data class SequenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)
