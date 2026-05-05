package org.meow.autistic.data.photo

import org.meow.autistic.data.foodlog.FoodCacheRepository
import org.meow.autistic.data.product.ProductRepository

/**
 * Resolves a product name or barcode to cached per-serving nutrition data.
 * Tries the food description cache first, then barcode lookup via external APIs.
 */
class FoodProductLookupService(
    private val foodCacheRepository: FoodCacheRepository,
    private val productRepository: ProductRepository,
) {
    suspend fun lookup(name: String?, barcode: String?): ParsedNutritionData? {
        if (name != null) {
            foodCacheRepository.findByDescription(name)?.let { return it }
        }
        if (barcode != null) {
            productRepository.getByBarcode(barcode)?.let { product ->
                val n = product.nutriments
                return ParsedNutritionData(
                    description = product.name,
                    servingSize = product.quantity,
                    barcode = product.barcode,
                    calories = n?.energyKcal?.value?.toDoubleOrNull() ?: 0.0,
                    protein = n?.proteins?.value?.toDoubleOrNull() ?: 0.0,
                    totalFat = n?.fat?.value?.toDoubleOrNull() ?: 0.0,
                    totalCarbs = n?.carbohydrates?.value?.toDoubleOrNull() ?: 0.0,
                    fiber = n?.fiber?.value?.toDoubleOrNull() ?: 0.0,
                    totalSugars = n?.sugars?.value?.toDoubleOrNull() ?: 0.0,
                    addedSugars = 0.0,
                    sugarAlcohols = 0.0,
                )
            }
        }
        return null
    }
}
