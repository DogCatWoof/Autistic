package org.meow.autistic.data.task

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A recurring daily task template.
 *
 * Each day the [DailyResetWorker] removes any unfinished todos that were generated
 * from daily tasks, then re-inserts them as new [] rows due today.
 *
 * @param timeMinutes Minutes since midnight for the due time (e.g. 8:30 AM = 510).
 *                    Null means no specific time — the todo will be due at day start.
 */
@Entity(tableName = "daily_tasks")
data class DailyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "General",
    val timeMinutes: Int? = null,
    /** Expected duration in minutes. Null means no estimate. */
    val expectedTimeMinutes: Int? = null,
    /** Propagated to each generated [TaskEntity] instance. */
    val isRequired: Boolean = false,
)
