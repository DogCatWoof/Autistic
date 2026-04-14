package org.meow.autistic.data.product

import android.os.SystemClock
import org.meow.autistic.data.diagnostics.QueryLogger

/**
 * Repository for local product lookups. Checks local DB first, then USDA FoodData Central,
 * then Open Food Facts as a fallback. Results are persisted in the local DB for future lookups.
 * Suspend calls are timed via [QueryLogger].
 */
class ProductRepository(
    private val dao: ProductDao,
    private val queryLogger: QueryLogger,
    private val usdaClient: UsdaFdcApiClient = UsdaFdcApiClient(),
    private val offClient: OpenFoodFactsApiClient = OpenFoodFactsApiClient(),
) {
    suspend fun getLocalByBarcode(barcode: String): ProductEntity? =
        timed("ProductRepository.getLocalByBarcode") { dao.getByBarcode(barcode) }

    suspend fun getByBarcode(barcode: String): ProductEntity? {
        getLocalByBarcode(barcode)?.let { return it }
        val result = usdaClient.fetchByBarcode(barcode)
            ?: offClient.fetchByBarcode(barcode)
            ?: return null
        timed("ProductRepository.upsertFromApi") { dao.upsertAll(listOf(result)) }
        return result
    }

    suspend fun upsertAll(products: List<ProductEntity>) =
        timed("ProductRepository.upsertAll") { dao.upsertAll(products) }

    private suspend inline fun <T> timed(label: String, block: suspend () -> T): T {
        val start = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            queryLogger.log(label, SystemClock.elapsedRealtime() - start)
        }
    }
}
