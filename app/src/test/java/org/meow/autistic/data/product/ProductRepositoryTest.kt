package org.meow.autistic.data.product

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.meow.autistic.data.diagnostics.QueryLogger

class ProductRepositoryTest {

    private val dao = mockk<ProductDao>(relaxed = true)
    private val usdaClient = mockk<UsdaFdcApiClient>(relaxed = true)
    private val offClient = mockk<OpenFoodFactsApiClient>(relaxed = true)
    private val repository = ProductRepository(dao, QueryLogger(), usdaClient, offClient)

    private val sampleEntity = ProductEntity(
        barcode = "0123456789",
        name = "Test Product",
        brands = null,
        quantity = null,
        servingsPerContainer = null,
        ingredients = null,
        foodGroups = null,
        nutriments = null,
    )

    // region getByBarcode

    @Test
    fun `getByBarcode returns entity when dao finds a match`() = runTest {
        coEvery { dao.getByBarcode("0123456789") } returns sampleEntity

        val result = repository.getByBarcode("0123456789")

        assertEquals(sampleEntity, result)
        coVerify(exactly = 0) { usdaClient.fetchByBarcode(any()) }
    }

    @Test
    fun `getByBarcode falls back to usda when dao finds no match`() = runTest {
        coEvery { dao.getByBarcode("unknown") } returns null
        coEvery { usdaClient.fetchByBarcode("unknown") } returns sampleEntity

        val result = repository.getByBarcode("unknown")

        assertEquals(sampleEntity, result)
    }

    @Test
    fun `getByBarcode saves usda result to dao on fallback hit`() = runTest {
        coEvery { dao.getByBarcode("unknown") } returns null
        coEvery { usdaClient.fetchByBarcode("unknown") } returns sampleEntity

        repository.getByBarcode("unknown")

        coVerify { dao.upsertAll(listOf(sampleEntity)) }
    }

    @Test
    fun `getByBarcode falls back to off when usda finds no match`() = runTest {
        coEvery { dao.getByBarcode("unknown") } returns null
        coEvery { usdaClient.fetchByBarcode("unknown") } returns null
        coEvery { offClient.fetchByBarcode("unknown") } returns sampleEntity

        assertEquals(sampleEntity, repository.getByBarcode("unknown"))
        coVerify { dao.upsertAll(listOf(sampleEntity)) }
    }

    @Test
    fun `getByBarcode returns null when dao usda and off all find nothing`() = runTest {
        coEvery { dao.getByBarcode("unknown") } returns null
        coEvery { usdaClient.fetchByBarcode("unknown") } returns null
        coEvery { offClient.fetchByBarcode("unknown") } returns null

        assertNull(repository.getByBarcode("unknown"))
    }

    @Test(expected = RuntimeException::class)
    fun `getByBarcode propagates dao exception`() = runTest {
        coEvery { dao.getByBarcode(any()) } throws RuntimeException("db error")
        repository.getByBarcode("any")
    }

    @Test(expected = java.io.IOException::class)
    fun `getByBarcode propagates usda IOException`() = runTest {
        coEvery { dao.getByBarcode("unknown") } returns null
        coEvery { usdaClient.fetchByBarcode("unknown") } throws java.io.IOException("network error")
        repository.getByBarcode("unknown")
    }

    // endregion

    // region upsertAll

    @Test
    fun `upsertAll delegates to dao`() = runTest {
        val list = listOf(sampleEntity)
        repository.upsertAll(list)
        coVerify { dao.upsertAll(list) }
    }

    @Test
    fun `upsertAll with empty list still calls dao`() = runTest {
        repository.upsertAll(emptyList())
        coVerify { dao.upsertAll(emptyList()) }
    }

    @Test(expected = RuntimeException::class)
    fun `upsertAll propagates dao exception`() = runTest {
        coEvery { dao.upsertAll(any()) } throws RuntimeException("db error")
        repository.upsertAll(listOf(sampleEntity))
    }

    // endregion
}
