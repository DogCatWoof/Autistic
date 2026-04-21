package org.meow.autistic.data.foodlog

import kotlinx.coroutines.flow.Flow

/** Persistence layer for food log daily totals and individual food item entries. */
class FoodLogRepository(private val dao: FoodLogDao, private val itemDao: FoodLogItemDao) {
    fun getByDate(date: String): Flow<FoodLogEntry?> = dao.getByDate(date)
    suspend fun upsert(entry: FoodLogEntry) = dao.upsert(entry)
    suspend fun deleteOlderThan(cutoffDate: String) = dao.deleteOlderThan(cutoffDate)

    fun getItemsByDate(date: String): Flow<List<FoodLogItemEntry>> = itemDao.getByDate(date)
    suspend fun insertItem(item: FoodLogItemEntry) = itemDao.insert(item)
    suspend fun deleteItem(item: FoodLogItemEntry) = itemDao.delete(item)
    suspend fun deleteItemsOlderThan(cutoffDate: String) = itemDao.deleteOlderThan(cutoffDate)
}
