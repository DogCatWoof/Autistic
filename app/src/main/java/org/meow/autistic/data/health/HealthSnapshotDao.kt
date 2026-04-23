package org.meow.autistic.data.health

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** DAO for [HealthSnapshotEntity]. */
@Dao
interface HealthSnapshotDao {
    @Query("SELECT * FROM health_snapshots WHERE date = :date")
    fun getByDate(date: String): Flow<HealthSnapshotEntity?>

    @Query("SELECT * FROM health_snapshots ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): HealthSnapshotEntity?

    @Upsert
    suspend fun upsert(entry: HealthSnapshotEntity)
}
