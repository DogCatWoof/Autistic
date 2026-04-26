package org.meow.autistic.data.sequence

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A single execution of a [SequenceEntity].
 * While [completedAt] is null the run is considered active.
 */
@Entity(tableName = "sequence_runs")
data class SequenceRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sequenceId: Long,
    val startedAt: Instant,
    val completedAt: Instant? = null,
)
