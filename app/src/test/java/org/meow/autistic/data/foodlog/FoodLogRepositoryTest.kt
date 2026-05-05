package org.meow.autistic.data.foodlog

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class FoodLogRepositoryTest {

    private lateinit var dao: FoodLogDao
    private lateinit var itemDao: FoodLogItemDao
    private lateinit var repository: FoodLogRepository

    private val item = FoodLogItemEntry(
        id = 1L,
        date = "2025-01-01",
        loggedAt = Instant.ofEpochMilli(1000L),
        calories = 200.0,
    )

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        itemDao = mockk(relaxed = true)
        repository = FoodLogRepository(dao, itemDao)
    }

    @Test
    fun `insertItem stamps pendingFirestoreSync and lastModifiedAt`() = runTest {
        coEvery { itemDao.insert(any()) } returns 1L
        repository.insertItem(item)
        coVerify { itemDao.insert(match { it.pendingFirestoreSync && it.calories == item.calories }) }
    }

    @Test
    fun `updateItem stamps pendingFirestoreSync and lastModifiedAt`() = runTest {
        repository.updateItem(item)
        coVerify { itemDao.update(match { it.pendingFirestoreSync && it.calories == item.calories }) }
    }

    @Test
    fun `deleteItem soft-deletes via update with isDeleted flag`() = runTest {
        repository.deleteItem(item)
        coVerify { itemDao.update(match { it.isDeleted && it.pendingFirestoreSync && it.id == item.id }) }
    }

    @Test
    fun `deleteItem does not call hard-delete dao method`() = runTest {
        repository.deleteItem(item)
        coVerify(exactly = 0) { itemDao.delete(any()) }
    }

    @Test
    fun `getItemById delegates to dao`() = runTest {
        coEvery { itemDao.getById(1L) } returns item
        val result = repository.getItemById(1L)
        assert(result == item)
    }
}
