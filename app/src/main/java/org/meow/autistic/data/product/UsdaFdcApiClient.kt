package org.meow.autistic.data.product

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.meow.autistic.BuildConfig

private const val USDA_SEARCH_URL =
    "https://api.nal.usda.gov/fdc/v1/foods/search?query={barcode}&dataType=Branded&api_key={key}"

/**
 * Fetches branded food data from the USDA FoodData Central REST API.
 */
class UsdaFdcApiClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val apiKey: String = BuildConfig.USDA_API_KEY,
) {
    /**
     * Looks up a product by [barcode] (UPC/EAN). Matches on the `gtinUpc` field so only
     * an exact barcode match is returned. Returns null when not found or on parse errors.
     * Throws [java.io.IOException] on network failure.
     */
    suspend fun fetchByBarcode(barcode: String): ProductEntity? = withContext(Dispatchers.IO) {
        val url = USDA_SEARCH_URL
            .replace("{barcode}", barcode)
            .replace("{key}", apiKey)
        val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) return@withContext null
        val body = response.body?.string() ?: return@withContext null
        runCatching {
            val root = JsonParser.parseString(body).asJsonObject
            val foods = root.getAsJsonArray("foods") ?: return@runCatching null
            foods.firstOrNull { food ->
                food.asJsonObject.str("gtinUpc")?.trimStart('0') == barcode.trimStart('0')
            }?.asJsonObject?.toProductEntity(barcode)
        }.getOrNull()
    }
}

private fun JsonObject.str(key: String): String? =
    get(key)?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }

private fun Double.fmt(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

private fun JsonObject.toProductEntity(barcode: String): ProductEntity? {
    val name = str("description") ?: return null
    return ProductEntity(
        barcode = barcode,
        name = name,
        brands = str("brandOwner") ?: str("brandName"),
        quantity = servingSize(),
        servingsPerContainer = servingsPerContainer(),
        ingredients = str("ingredients"),
        foodGroups = str("foodCategory"),
        nutriments = parseFoodNutrients(),
    )
}

private fun JsonObject.servingSize(): String? {
    val household = str("householdServingFullText")
    val size = get("servingSize")?.takeIf { !it.isJsonNull }?.asDouble
    val unit = str("servingSizeUnit")
    return when {
        household != null && size != null && unit != null -> "$household (${size.fmt()} $unit)"
        household != null -> household
        size != null && unit != null -> "${size.fmt()} $unit"
        else -> null
    }
}

private fun JsonObject.servingsPerContainer(): String? =
    get("numberOfServingsPerPackage")
        ?.takeIf { !it.isJsonNull && it.isJsonPrimitive }
        ?.asDouble
        ?.fmt()

private fun JsonObject.parseFoodNutrients(): Nutriments? {
    val array = get("foodNutrients")?.takeIf { it.isJsonArray }?.asJsonArray ?: return null
    val map = buildMap<String, NutrientValue> {
        for (el in array) {
            val obj = el.asJsonObject
            val nutrientName = obj.str("nutrientName") ?: continue
            val rawValue = obj.get("value")?.takeIf { !it.isJsonNull }?.asDouble ?: continue
            val unit = obj.str("unitName") ?: continue
            val pct = obj.get("percentDailyValue")?.takeIf { !it.isJsonNull }?.asInt
            put(nutrientName.lowercase(), NutrientValue(rawValue.fmt(), unit, pct))
        }
    }
    if (map.isEmpty()) return null
    return Nutriments(
        energyKcal = map["energy"],
        fat = map["total lipid (fat)"],
        saturatedFat = map["fatty acids, total saturated"],
        transFat = map["fatty acids, total trans"],
        polyunsaturatedFat = map["fatty acids, total polyunsaturated"],
        monounsaturatedFat = map["fatty acids, total monounsaturated"],
        cholesterol = map["cholesterol"],
        sodium = map["sodium, na"],
        carbohydrates = map["carbohydrate, by difference"],
        sugars = map["total sugars"],
        fiber = map["fiber, total dietary"],
        proteins = map["protein"],
        vitaminA = map["vitamin a, iu"],
        vitaminD = map["vitamin d (d2 + d3), international units"],
        calcium = map["calcium, ca"],
        iron = map["iron, fe"],
        potassium = map["potassium, k"],
        magnesium = map["magnesium, mg"],
        phosphorus = map["phosphorus, p"],
        riboflavin = map["riboflavin"],
        folate = map["folate, total"],
        vitaminB12 = map["vitamin b-12"],
    )
}
