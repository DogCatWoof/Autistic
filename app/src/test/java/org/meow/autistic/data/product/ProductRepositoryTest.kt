package org.meow.autistic.data.product

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.meow.autistic.data.diagnostics.QueryLogger

class ProductRepositoryTest {

    private val dao = mockk<ProductDao>(relaxed = true)
    private val repository = ProductRepository(dao, QueryLogger())

    private val sampleEntity = ProductEntity(barcode = "0123456789", productJson = """{"code":"0123456789","product_name":"Test"}""")

    // region getByBarcode

    @Test
    fun `getByBarcode returns entity when dao finds a match`() = runTest {
        coEvery { dao.getByBarcode("0123456789") } returns sampleEntity

        val result = repository.getByBarcode("0123456789")

        assertEquals(sampleEntity, result)
    }

    @Test
    fun `getByBarcode returns null when dao finds no match`() = runTest {
        coEvery { dao.getByBarcode("unknown") } returns null

        assertNull(repository.getByBarcode("unknown"))
    }

    @Test(expected = RuntimeException::class)
    fun `getByBarcode propagates dao exception`() = runTest {
        coEvery { dao.getByBarcode(any()) } throws RuntimeException("db error")
        repository.getByBarcode("any")
    }

    // endregion

    // region hasProducts

    @Test
    fun `hasProducts returns true when count is positive`() = runTest {
        coEvery { dao.count() } returns 42L

        assertTrue(repository.hasProducts())
    }

    @Test
    fun `hasProducts returns false when count is zero`() = runTest {
        coEvery { dao.count() } returns 0L

        assertFalse(repository.hasProducts())
    }

    @Test(expected = RuntimeException::class)
    fun `hasProducts propagates dao exception`() = runTest {
        coEvery { dao.count() } throws RuntimeException("db error")
        repository.hasProducts()
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
