package org.meow.autistic.data.keto

import kotlinx.coroutines.flow.Flow

/** Persistence layer for keto daily totals and individual food item entries. */
class KetoRepository(private val dao: KetoDao, private val itemDao: KetoItemDao) {
    fun getByDate(date: String): Flow<KetoLogEntry?> = dao.getByDate(date)
    suspend fun upsert(entry: KetoLogEntry) = dao.upsert(entry)
    suspend fun deleteOlderThan(cutoffDate: String) = dao.deleteOlderThan(cutoffDate)

    fun getItemsByDate(date: String): Flow<List<KetoItemEntry>> = itemDao.getByDate(date)
    suspend fun insertItem(item: KetoItemEntry) = itemDao.insert(item)
    suspend fun deleteItem(item: KetoItemEntry) = itemDao.delete(item)
    suspend fun deleteItemsOlderThan(cutoffDate: String) = itemDao.deleteOlderThan(cutoffDate)
}
