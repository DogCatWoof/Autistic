package org.meow.autistic.data.photo

enum class PhotoType { FOOD, PRODUCT, UNKNOWN }

/** Result of classifying a photo before deciding on the nutrition lookup path. */
data class PhotoClassificationResult(
    val type: PhotoType,
    val name: String?,
    val barcode: String?,
    val nutrition: ParsedNutritionData?,
)
