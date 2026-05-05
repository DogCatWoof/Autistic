package org.meow.autistic.data.conversation

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Loads response templates from the bundled JSON asset and caches them in memory.
 * Returns the three templates for the given [intentClass] and [tone] (neutral/polite/direct).
 * Falls back to [IntentClass.Unknown] templates when the class has no entries.
 */
class ResponseTemplateRepository(private val context: Context) {

    private val cache: Map<String, Map<String, List<String>>> by lazy { loadAsset() }

    fun getTemplates(intentClass: IntentClass, tone: String): List<ResponseTemplate> {
        val byClass = cache[intentClass.name] ?: cache[IntentClass.Unknown.name] ?: return emptyList()
        val texts = byClass[tone] ?: byClass["neutral"] ?: return emptyList()
        return texts.map { ResponseTemplate(it, tone) }
    }

    private fun loadAsset(): Map<String, Map<String, List<String>>> {
        val json = context.assets.open("response_templates.json").bufferedReader().readText()
        val type = object : TypeToken<Map<String, Map<String, List<String>>>>() {}.type
        return Gson().fromJson(json, type)
    }
}
