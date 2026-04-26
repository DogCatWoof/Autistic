package org.meow.autistic.data.sequence

import androidx.room.Entity
import java.time.Instant

/** Records a single step completion within a [SequenceRunEntity]. */
@Entity(tableName = "sequence_step_progress", primaryKeys = ["runId", "stepId"])
data class SequenceStepProgressEntity(
    val runId: Long,
    val stepId: Long,
    val completedAt: Instant,
)
