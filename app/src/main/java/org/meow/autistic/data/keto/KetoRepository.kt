package org.meow.autistic.data.keto

import kotlinx.coroutines.flow.Flow

/** Persistence layer for daily keto totals. */
class KetoRepository(private val dao: KetoDao) {
    fun getByDate(date: String): Flow<KetoLogEntry?> = dao.getByDate(date)
    suspend fun upsert(entry: KetoLogEntry) = dao.upsert(entry)
    suspend fun deleteOlderThan(cutoffDate: String) = dao.deleteOlderThan(cutoffDate)
}
