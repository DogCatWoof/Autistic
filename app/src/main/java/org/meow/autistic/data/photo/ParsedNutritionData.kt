package org.meow.autistic.data.photo

/**
 * Nutrition values parsed from a nutrition facts label, all expressed per one serving.
 */
data class ParsedNutritionData(
    val description: String? = null,
    val servingSize: String? = null,
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val fiber: Double = 0.0,
    val totalSugars: Double = 0.0,
    val addedSugars: Double = 0.0,
    val sugarAlcohols: Double = 0.0,
    val barcode: String? = null,
)
