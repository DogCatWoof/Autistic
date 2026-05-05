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

    @Query("SELECT * FROM health_snapshots ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<HealthSnapshotEntity>>

    @Upsert
    suspend fun upsert(entry: HealthSnapshotEntity)

    @Query("SELECT * FROM health_snapshots WHERE pendingFirestoreSync = 1")
    suspend fun getPendingFirestoreSync(): List<HealthSnapshotEntity>

    @Query("UPDATE health_snapshots SET pendingFirestoreSync = 0, firestoreId = :firestoreId WHERE date = :date")
    suspend fun markFirestoreSynced(date: String, firestoreId: String)

    @Query("SELECT * FROM health_snapshots WHERE date = :date")
    suspend fun getByDateOnce(date: String): HealthSnapshotEntity?
}
