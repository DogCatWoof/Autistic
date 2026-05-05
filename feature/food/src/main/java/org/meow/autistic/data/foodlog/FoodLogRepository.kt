package org.meow.autistic.data.foodlog

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Persistence layer for food log daily totals and individual food item entries. */
class FoodLogRepository(private val dao: FoodLogDao, private val itemDao: FoodLogItemDao) {
    fun getByDate(date: String): Flow<FoodLogEntry?> = dao.getByDate(date)
    suspend fun upsert(entry: FoodLogEntry) = dao.upsert(entry)
    suspend fun deleteOlderThan(cutoffDate: String) = dao.deleteOlderThan(cutoffDate)

    fun getItemsByDate(date: String): Flow<List<FoodLogItemEntry>> = itemDao.getByDate(date)
    suspend fun insertItem(item: FoodLogItemEntry): Long =
        itemDao.insert(item.copy(lastModifiedAt = Instant.now(), pendingFirestoreSync = true))

    suspend fun updateItem(item: FoodLogItemEntry) =
        itemDao.update(item.copy(lastModifiedAt = Instant.now(), pendingFirestoreSync = true))

    suspend fun getItemById(id: Long): FoodLogItemEntry? = itemDao.getById(id)
    suspend fun getPendingAnalysisItems(): List<FoodLogItemEntry> = itemDao.getPendingAnalysis()

    suspend fun deleteItem(item: FoodLogItemEntry) =
        itemDao.update(item.copy(isDeleted = true, lastModifiedAt = Instant.now(), pendingFirestoreSync = true))
    suspend fun deleteItemsOlderThan(cutoffDate: String) = itemDao.deleteOlderThan(cutoffDate)
}
