package org.meow.autistic.data.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

private const val ANTHROPIC_API_KEY = "ANTHROPIC_KEY_REDACTED"
private const val ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages"
private const val MODEL = "claude-haiku-4-5-20251001"
private const val MAX_IMAGE_DIM = 1024

private const val CLASSIFY_PROMPT = "Analyze this image and classify it. " +
    "If it shows a meal, prepared dish, or raw ingredient: type = \"food\". " +
    "If it shows a packaged product with a visible barcode or nutrition label: type = \"product\". " +
    "Otherwise: type = \"unknown\". " +
    "Return ONLY valid JSON, no other text: " +
    "{\"type\":\"food\",\"name\":null,\"barcode\":null,\"serving_size\":null," +
    "\"calories\":0,\"protein_g\":0,\"total_fat_g\":0,\"total_carbs_g\":0," +
    "\"fiber_g\":0,\"total_sugars_g\":0,\"added_sugars_g\":0,\"sugar_alcohols_g\":0}. " +
    "Rules: type is exactly \"food\", \"product\", or \"unknown\". " +
    "name: food or product name, null if unrecognizable. " +
    "barcode: numeric barcode digits if clearly visible, otherwise null. " +
    "serving_size: estimated portion for food (e.g. \"1 plate\", \"200g\"), null for product/unknown. " +
    "Nutrient fields: estimated per-serving values for food type, all 0 for product/unknown. " +
    "All numeric fields must be numbers, never null."

private const val NUTRITION_LABEL_PROMPT = "Parse this nutrition facts label. " +
    "Return ONLY valid JSON, no other text: " +
    "{\"serving_size\":\"...\",\"calories\":0,\"protein_g\":0,\"total_fat_g\":0," +
    "\"total_carbs_g\":0,\"fiber_g\":0,\"total_sugars_g\":0,\"added_sugars_g\":0," +
    "\"sugar_alcohols_g\":0}. All nutrient fields must be numbers. " +
    "Use 0 for missing fields. serving_size may be null if not visible."

/**
 * Calls the Anthropic Messages API with a photo to classify food images and parse nutrition labels.
 */
class ClaudeVisionClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val apiKey: String = ANTHROPIC_API_KEY,
) {
    suspend fun classifyAndAnalyze(imagePath: String): PhotoClassificationResult = withContext(Dispatchers.IO) {
        parseClassificationJson(callApi(imagePath, CLASSIFY_PROMPT))
    }

    suspend fun analyzeNutritionLabel(imagePath: String): ParsedNutritionData = withContext(Dispatchers.IO) {
        parseNutritionJson(callApi(imagePath, NUTRITION_LABEL_PROMPT))
    }

    private fun callApi(imagePath: String, prompt: String): String {
        val imageBase64 = encodeImage(imagePath).ifEmpty { throw RuntimeException("Failed to encode image at $imagePath") }
        val body = buildRequestBody(imageBase64, prompt)
        val request = Request.Builder()
            .url(ANTHROPIC_API_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "no response body"
            throw RuntimeException("HTTP ${response.code}: $errorBody")
        }
        val responseBody = response.body?.string() ?: throw RuntimeException("Empty response body from API")
        return JsonParser.parseString(responseBody).asJsonObject
            .getAsJsonArray("content")?.firstOrNull()?.asJsonObject?.get("text")?.asString
            ?: throw RuntimeException("No text content in API response")
    }

    private fun encodeImage(path: String): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_IMAGE_DIM || bounds.outHeight / sampleSize > MAX_IMAGE_DIM) {
            sampleSize *= 2
        }
        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            ?: return ""
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    private fun buildRequestBody(imageBase64: String, prompt: String): JsonObject =
        JsonObject().apply {
            addProperty("model", MODEL)
            addProperty("max_tokens", 1024)
            add("messages", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("content", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("type", "image")
                            add("source", JsonObject().apply {
                                addProperty("type", "base64")
                                addProperty("media_type", "image/jpeg")
                                addProperty("data", imageBase64)
                            })
                        })
                        add(JsonObject().apply {
                            addProperty("type", "text")
                            addProperty("text", prompt)
                        })
                    })
                })
            })
        }

    private fun parseClassificationJson(text: String): PhotoClassificationResult {
        val jsonText = text.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JsonParser.parseString(jsonText).asJsonObject
        val type = when (json.get("type")?.asString?.lowercase()) {
            "food" -> PhotoType.FOOD
            "product" -> PhotoType.PRODUCT
            else -> PhotoType.UNKNOWN
        }
        val name = json.get("name")?.takeIf { !it.isJsonNull }?.asString
        val barcode = json.get("barcode")?.takeIf { !it.isJsonNull }?.asString
        val nutrition = if (type == PhotoType.FOOD) {
            ParsedNutritionData(
                description = name,
                servingSize = json.get("serving_size")?.takeIf { !it.isJsonNull }?.asString,
                calories = json.dbl("calories"),
                protein = json.dbl("protein_g"),
                totalFat = json.dbl("total_fat_g"),
                totalCarbs = json.dbl("total_carbs_g"),
                fiber = json.dbl("fiber_g"),
                totalSugars = json.dbl("total_sugars_g"),
                addedSugars = json.dbl("added_sugars_g"),
                sugarAlcohols = json.dbl("sugar_alcohols_g"),
            )
        } else null
        return PhotoClassificationResult(type, name, barcode, nutrition)
    }

    private fun parseNutritionJson(text: String): ParsedNutritionData {
        val jsonText = text.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JsonParser.parseString(jsonText).asJsonObject
        return ParsedNutritionData(
            servingSize = json.get("serving_size")?.takeIf { !it.isJsonNull }?.asString,
            calories = json.dbl("calories"),
            protein = json.dbl("protein_g"),
            totalFat = json.dbl("total_fat_g"),
            totalCarbs = json.dbl("total_carbs_g"),
            fiber = json.dbl("fiber_g"),
            totalSugars = json.dbl("total_sugars_g"),
            addedSugars = json.dbl("added_sugars_g"),
            sugarAlcohols = json.dbl("sugar_alcohols_g"),
        )
    }
}

private fun JsonObject.dbl(key: String): Double =
    get(key)?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
