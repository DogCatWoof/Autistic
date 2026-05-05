package org.meow.autistic.data.sequence

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** A single ordered step within a [SequenceEntity]. */
@Entity(tableName = "sequence_steps")
data class SequenceStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sequenceId: Long,
    val instruction: String,
    val estimatedMinutes: Int? = null,
    val position: Int,
    val firestoreId: String? = null,
    val lastModifiedAt: Instant = Instant.now(),
    val pendingFirestoreSync: Boolean = true,
    val isDeleted: Boolean = false,
)
