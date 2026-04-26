package org.meow.autistic.data.sequence

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single ordered step within a [SequenceEntity]. */
@Entity(tableName = "sequence_steps")
data class SequenceStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sequenceId: Long,
    val instruction: String,
    val estimatedMinutes: Int? = null,
    val position: Int,
)
