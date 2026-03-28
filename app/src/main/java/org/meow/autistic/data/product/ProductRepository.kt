package org.meow.autistic.data.product

import android.os.SystemClock
import org.meow.autistic.data.diagnostics.QueryLogger

/**
 * Repository for local product lookups against the Open Food Facts database.
 * Falls back to the OFF REST API when a barcode is absent from the local DB,
 * then persists the result for future lookups.
 * Suspend calls are timed via [QueryLogger].
 */
class ProductRepository(
    private val dao: ProductDao,
    private val queryLogger: QueryLogger,
    private val apiClient: OpenFoodFactsApiClient,
) {
    suspend fun getByBarcode(barcode: String): ProductEntity? {
        timed("ProductRepository.getByBarcode") { dao.getByBarcode(barcode) }
            ?.let { return it }
        val remote = apiClient.fetchByBarcode(barcode) ?: return null
        timed("ProductRepository.upsertFromApi") { dao.upsertAll(listOf(remote)) }
        return remote
    }

    suspend fun hasProducts(): Boolean =
        timed("ProductRepository.hasProducts") { dao.count() > 0 }

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
