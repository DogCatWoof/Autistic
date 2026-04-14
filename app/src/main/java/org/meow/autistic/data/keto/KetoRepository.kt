package org.meow.autistic.data.keto

import kotlinx.coroutines.flow.Flow

/**
 * Persistence layer for [KetoLogEntry] records.
 */
class KetoRepository(private val dao: KetoDao) {
    fun getByDate(date: String): Flow<List<KetoLogEntry>> = dao.getByDate(date)
    suspend fun insert(entry: KetoLogEntry) = dao.insert(entry)
    suspend fun delete(entry: KetoLogEntry) = dao.delete(entry)
}
