package org.meow.autistic.data.product

/** Repository for local product lookups against the Open Food Facts database. */
class ProductRepository(private val dao: ProductDao) {

    suspend fun getByBarcode(barcode: String): ProductEntity? = dao.getByBarcode(barcode)

    suspend fun hasProducts(): Boolean = dao.count() > 0

    suspend fun upsertAll(products: List<ProductEntity>) = dao.upsertAll(products)
}
