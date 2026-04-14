package org.meow.autistic.data.product

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private const val OFF_PRODUCT_URL = "https://world.openfoodfacts.net/api/v2/product/{barcode}"

/**
 * Fetches individual products from the Open Food Facts REST API.
 * Used as a fallback when USDA FDC has no entry for a barcode.
 * Throws [java.io.IOException] on network failure.
 */
class OpenFoodFactsApiClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    suspend fun fetchByBarcode(barcode: String): ProductEntity? = withContext(Dispatchers.IO) {
        val url = OFF_PRODUCT_URL.replace("{barcode}", barcode)
        val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) return@withContext null
        val body = response.body?.string() ?: return@withContext null
        runCatching {
            val root = JsonParser.parseString(body).asJsonObject
            if (root.get("status")?.asInt != 1) return@runCatching null
            root.getAsJsonObject("product")?.toProductEntity(barcode)
        }.getOrNull()
    }
}

private fun JsonObject.str(key: String): String? =
    get(key)?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }

/** Reads a nutrient from the nested `nutrients` format: `{value_string, unit}`. */
private fun JsonObject.nutrient(key: String): NutrientValue? {
    val obj = get(key)?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
    val value = obj.str("value_string") ?: return null
    val unit = obj.str("unit") ?: return null
    return NutrientValue(value, unit)
}

/** Reads a nutrient from the flat `nutriments` format: numeric value + `key_unit`. */
private fun JsonObject.flatNutrient(key: String): NutrientValue? {
    val el = get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive } ?: return null
    val value = el.asString.takeIf { it.isNotBlank() } ?: return null
    val unit = str("${key}_unit") ?: return null
    return NutrientValue(value, unit)
}

private fun JsonObject.parseNutriments(): Nutriments? {
    val nested = get("nutrients")?.takeIf { it.isJsonObject }?.asJsonObject
    val flat = get("nutriments")?.takeIf { it.isJsonObject }?.asJsonObject
    if (nested == null && flat == null) return null
    fun pick(key: String): NutrientValue? = nested?.nutrient(key) ?: flat?.flatNutrient(key)
    return Nutriments(
        energyKcal     = pick("energy-kcal"),
        fat            = pick("fat"),
        saturatedFat   = pick("saturated-fat"),
        transFat       = pick("trans-fat"),
        polyunsaturatedFat  = pick("polyunsaturated-fat"),
        monounsaturatedFat  = pick("monounsaturated-fat"),
        cholesterol    = pick("cholesterol"),
        sodium         = pick("sodium") ?: pick("salt"),
        carbohydrates  = pick("carbohydrates"),
        sugars         = pick("sugars"),
        fiber          = pick("fiber"),
        proteins       = pick("proteins"),
        vitaminA       = pick("vitamin-a"),
        vitaminD       = pick("vitamin-d"),
        calcium        = pick("calcium"),
        iron           = pick("iron"),
        potassium      = pick("potassium"),
        magnesium      = pick("magnesium"),
        phosphorus     = pick("phosphorus"),
        riboflavin     = pick("riboflavin") ?: pick("vitamin-b2"),
        folate         = pick("folate") ?: pick("folates"),
        vitaminB12     = pick("vitamin-b12"),
    )
}

private fun JsonObject.toProductEntity(barcode: String): ProductEntity? {
    val name = str("product_name") ?: return null
    return ProductEntity(
        barcode              = barcode,
        name                 = name,
        brands               = str("brands"),
        quantity             = str("quantity"),
        servingsPerContainer = str("servings_per_container"),
        ingredients          = str("ingredients_text"),
        foodGroups           = str("food_groups"),
        nutriments           = parseNutriments(),
    )
}
