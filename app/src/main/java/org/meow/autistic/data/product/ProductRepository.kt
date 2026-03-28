package org.meow.autistic.data.product

import android.os.SystemClock
import org.meow.autistic.data.diagnostics.QueryLogger

/**
 * Repository for local product lookups against the Open Food Facts database.
 * Suspend calls are timed via [QueryLogger].
 */
class ProductRepository(
    private val dao: ProductDao,
    private val queryLogger: QueryLogger,
) {
    suspend fun getByBarcode(barcode: String): ProductEntity? =
        timed("ProductRepository.getByBarcode") { dao.getByBarcode(barcode) }

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
