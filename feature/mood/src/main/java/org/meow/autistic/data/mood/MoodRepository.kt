package org.meow.autistic.data.mood

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Repository for mood readings. Single source of truth over [MoodDao]. */
class MoodRepository(private val dao: MoodDao) {
    fun getAll(): Flow<List<MoodEntity>> = dao.getAll()

    suspend fun insert(mood: MoodEntity) =
        dao.insert(mood.copy(lastModifiedAt = Instant.now(), pendingFirestoreSync = true))

    suspend fun delete(mood: MoodEntity) =
        dao.upsert(mood.copy(isDeleted = true, lastModifiedAt = Instant.now(), pendingFirestoreSync = true))
}
