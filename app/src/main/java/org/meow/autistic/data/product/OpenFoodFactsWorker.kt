package org.meow.autistic.data.product

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

const val OFF_SYNC_WORK_NAME = "off_product_sync"

private const val OFF_JSONL_URL =
    "https://static.openfoodfacts.org/data/openfoodfacts-products.jsonl.gz"

private const val OFF_DELTA_INDEX_URL =
    "https://static.openfoodfacts.org/data/delta/index.txt"

private const val OFF_DELTA_BASE_URL =
    "https://static.openfoodfacts.org/data/delta/"

/** Fetch a single product by barcode: replace {barcode} with the scanned value. */
internal const val OFF_PRODUCT_URL = "https://world.openfoodfacts.net/api/v2/product/{barcode}"

/** Add or update a product (POST). Requires `code`, `user_id`, and `password` in the body. */
internal const val OFF_ADD_PRODUCT_URL = "https://world.openfoodfacts.net/cgi/product_jqm2.pl"

private const val BATCH_SIZE = 500
private val LAST_SYNC_KEY = longPreferencesKey("off_last_sync")
private val Context.productDataStore: DataStore<Preferences> by preferencesDataStore("product_prefs")

// Matches filenames like: openfoodfacts_products_1774596577_1774687161.json.gz
internal val DELTA_FILENAME_REGEX = Regex("""openfoodfacts_products_(\d+)_(\d+)\.json\.gz""")

/**
 * CoroutineWorker that downloads and imports the Open Food Facts product database.
 *
 * On first run (empty table) it streams the full `.jsonl.gz` dump.
 * On subsequent runs it fetches only the delta files published since the last
 * successful sync, using the index at [OFF_DELTA_INDEX_URL]. Delta filenames
 * encode start/end Unix timestamps; files whose end-timestamp exceeds the last
 * sync time are applied in ascending order. If no applicable delta files exist
 * (e.g. last sync older than 14 days), it falls back to a full download.
 *
 * All streams are decompressed via [GZIPInputStream] without writing to disk.
 * Rows are upserted in batches of [BATCH_SIZE]. Progress is reported via
 * [setProgress]; the completion timestamp is stored in DataStore on success.
 */
class OpenFoodFactsWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: ProductRepository by inject()
    private val gson = Gson()
    private val client = OkHttpClient()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val lastSync = context.productDataStore.data.first()[LAST_SYNC_KEY]

            if (!repository.hasProducts() || lastSync == null) {
                setProgress(status("Full download starting…"))
                importUrls(listOf(OFF_JSONL_URL))
            } else {
                val deltaUrls = fetchDeltaUrls(lastSync)
                if (deltaUrls.isEmpty()) {
                    setProgress(status("No recent deltas; falling back to full download…"))
                    importUrls(listOf(OFF_JSONL_URL))
                } else {
                    setProgress(status("Applying ${deltaUrls.size} delta file(s)…"))
                    importUrls(deltaUrls)
                }
            }

            context.productDataStore.edit { prefs -> prefs[LAST_SYNC_KEY] = System.currentTimeMillis() }
            Result.success()
        } catch (e: Exception) {
            Result.failure(Data.Builder().putString("error", e.message).build())
        }
    }

    /**
     * Fetches [OFF_DELTA_INDEX_URL] and returns sorted URLs for delta files
     * whose end-timestamp is after [lastSyncMs]. Returns empty list on HTTP error,
     * which triggers a full-download fallback in [doWork].
     */
    private suspend fun fetchDeltaUrls(lastSyncMs: Long): List<String> {
        val response = client.newCall(Request.Builder().url(OFF_DELTA_INDEX_URL).build()).execute()
        if (!response.isSuccessful) return emptyList()
        val body = response.body?.string() ?: return emptyList()
        return parseDeltaUrls(body, lastSyncMs)
    }

    private suspend fun importUrls(urls: List<String>) {
        var totalImported = 0L
        for ((index, url) in urls.withIndex()) {
            val label = if (urls.size > 1) " (${index + 1} of ${urls.size})" else ""
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code} fetching $url")
            val body = response.body ?: throw IllegalStateException("Empty body for $url")
            val contentLengthMb = body.contentLength().let { if (it > 0) it / 1_000_000 else -1L }
            var bytesRead = 0L
            val countingStream = object : java.io.InputStream() {
                private val raw = body.byteStream()
                override fun read(): Int = raw.read().also { if (it >= 0) bytesRead++ }
                override fun read(b: ByteArray, off: Int, len: Int): Int =
                    raw.read(b, off, len).also { if (it > 0) bytesRead += it }
                override fun close() = raw.close()
            }
            val reader = BufferedReader(InputStreamReader(GZIPInputStream(countingStream)))
            totalImported += processStream(reader, totalImported, label, contentLengthMb) { bytesRead }
        }
    }

    private suspend fun processStream(
        reader: BufferedReader,
        startCount: Long,
        label: String,
        contentLengthMb: Long,
        bytesRead: () -> Long,
    ): Long {
        val batch = mutableListOf<ProductEntity>()
        var imported = 0L
        var line = reader.readLine()
        while (line != null) {
            val obj = runCatching { gson.fromJson(line, JsonObject::class.java) }.getOrNull()
            val barcode = obj?.get("code")?.asString?.trim() ?: ""
            if (barcode.isNotBlank()) {
                val map = HashMap<String, String>(obj!!.size())
                for ((key, value) in obj.entrySet()) {
                    if (!value.isJsonNull) {
                        map[key] = if (value.isJsonPrimitive) value.asString else value.toString()
                    }
                }
                batch.add(ProductEntity(barcode = barcode, productJson = gson.toJson(map)))
                if (batch.size >= BATCH_SIZE) {
                    repository.upsertAll(batch.toList())
                    imported += batch.size
                    batch.clear()
                    val mbSoFar = bytesRead() / 1_000_000
                    val progressStr = if (contentLengthMb > 0) "$mbSoFar MB of $contentLengthMb MB"
                                      else "$mbSoFar MB"
                    setProgress(status("Downloading$label $progressStr · ${startCount + imported} products"))
                }
            }
            line = reader.readLine()
        }
        if (batch.isNotEmpty()) {
            repository.upsertAll(batch.toList())
            imported += batch.size
        }
        return imported
    }

    private fun status(msg: String): Data = Data.Builder().putString("status", msg).build()

    companion object {
        /** Emits the last successful sync timestamp, or null if never synced. */
        fun getLastSyncFlow(context: Context): Flow<Long?> =
            context.productDataStore.data.map { prefs -> prefs[LAST_SYNC_KEY] }

        suspend fun getLastSyncTime(context: Context): Long? =
            context.productDataStore.data.first()[LAST_SYNC_KEY]

        /**
         * Parses [indexBody] (the content of index.txt) and returns sorted delta
         * URLs whose end-timestamp exceeds [lastSyncMs] (stored in milliseconds).
         */
        internal fun parseDeltaUrls(indexBody: String, lastSyncMs: Long): List<String> {
            val lastSyncSec = lastSyncMs / 1000L
            return indexBody.lines()
                .mapNotNull { line ->
                    val filename = line.trim()
                    val match = DELTA_FILENAME_REGEX.find(filename) ?: return@mapNotNull null
                    val endTimestamp = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                    if (endTimestamp > lastSyncSec) OFF_DELTA_BASE_URL + filename else null
                }
                .sorted()
        }
    }
}
