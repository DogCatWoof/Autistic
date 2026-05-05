package org.meow.autistic.data.health

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** One day's aggregated Health Connect metrics, keyed by local date string (yyyy-MM-dd). */
@Entity(tableName = "health_snapshots")
data class HealthSnapshotEntity(
    @PrimaryKey val date: String,
    val steps: Long? = null,
    val sleepMinutes: Long? = null,
    val avgHeartRateBpm: Double? = null,
    val weightKg: Double? = null,
    val caloriesBurned: Double? = null,
    val bloodGlucoseMmol: Double? = null,
    val lastUpdatedAt: Instant = Instant.now(),
    val firestoreId: String? = null,
    val pendingFirestoreSync: Boolean = true,
)
