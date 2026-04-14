package org.meow.autistic.data.mood

import kotlinx.coroutines.flow.Flow

/** Repository for mood readings. Single source of truth over [MoodDao]. */
class MoodRepository(private val dao: MoodDao) {
    fun getAll(): Flow<List<MoodEntity>> = dao.getAll()
    suspend fun insert(mood: MoodEntity) = dao.insert(mood)
    suspend fun delete(mood: MoodEntity) = dao.delete(mood)
}
