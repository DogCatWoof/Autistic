package org.meow.autistic.data.product

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches individual products from the Open Food Facts REST API.
 * Network I/O is dispatched on [Dispatchers.IO].
 */
class OpenFoodFactsApiClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val gson = Gson()

    /**
     * Looks up a product by [barcode]. Returns a [ProductEntity] when the API
     * reports status 1 (product found), or null for not-found or any error.
     */
    suspend fun fetchByBarcode(barcode: String): ProductEntity? = withContext(Dispatchers.IO) {
        runCatching {
            val url = OFF_PRODUCT_URL.replace("{barcode}", barcode)
            val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) return@runCatching null
            val body = response.body?.string() ?: return@runCatching null
            val root = gson.fromJson(body, JsonObject::class.java)
            if (root.get("status")?.asInt != 1) return@runCatching null
            val product = root.getAsJsonObject("product") ?: return@runCatching null
            val map = HashMap<String, String>(product.size())
            for ((key, value) in product.entrySet()) {
                if (!value.isJsonNull) {
                    map[key] = if (value.isJsonPrimitive) value.asString else value.toString()
                }
            }
            ProductEntity(barcode = barcode, productJson = gson.toJson(map))
        }.getOrNull()
    }
}
