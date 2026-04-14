package org.meow.autistic.data.product

/** A single nutrient value with its display unit and optional % Daily Value from the API. */
data class NutrientValue(val value: String, val unit: String, val dailyValuePercent: Int? = null)

/** Nutrition values per serving, sourced from USDA FDC. */
data class Nutriments(
    // Macros — main label section
    val energyKcal: NutrientValue?,
    val fat: NutrientValue?,
    val saturatedFat: NutrientValue?,
    val transFat: NutrientValue?,
    val polyunsaturatedFat: NutrientValue?,
    val monounsaturatedFat: NutrientValue?,
    val cholesterol: NutrientValue?,
    val sodium: NutrientValue?,
    val carbohydrates: NutrientValue?,
    val sugars: NutrientValue?,
    val fiber: NutrientValue?,
    val proteins: NutrientValue?,
    // Vitamins & minerals — second table
    val vitaminA: NutrientValue?,
    val vitaminD: NutrientValue?,
    val calcium: NutrientValue?,
    val iron: NutrientValue?,
    val potassium: NutrientValue?,
    val magnesium: NutrientValue?,
    val phosphorus: NutrientValue?,
    val riboflavin: NutrientValue?,
    val folate: NutrientValue?,
    val vitaminB12: NutrientValue?,
)
