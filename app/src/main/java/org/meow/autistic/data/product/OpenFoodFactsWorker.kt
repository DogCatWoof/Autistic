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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.meow.autistic.data.todo.TodoDatabase
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

const val OFF_SYNC_WORK_NAME = "off_product_sync"

private const val OFF_CSV_URL =
    "https://static.openfoodfacts.org/data/en.openfoodfacts.org.products.csv.gz"
private const val BATCH_SIZE = 500
private val LAST_SYNC_KEY = longPreferencesKey("off_last_sync")
private val Context.productDataStore: DataStore<Preferences> by preferencesDataStore("product_prefs")

/**
 * CoroutineWorker that downloads and imports the Open Food Facts product database.
 *
 * Streams the .csv.gz file through [GZIPInputStream] without writing to disk,
 * parses the tab-separated CSV, and upserts rows in batches of [BATCH_SIZE].
 * Reports live progress via [setProgress] and stores the completion timestamp
 * in DataStore on success.
 */
class OpenFoodFactsWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val gson = Gson()
    private val repository = ProductRepository(TodoDatabase.getDatabase(context).productDao())

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            setProgress(Data.Builder().putString("status", "Downloading…").build())

            val client = OkHttpClient()
            val request = Request.Builder().url(OFF_CSV_URL).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Data.Builder().putString("error", "HTTP ${response.code}").build()
                )
            }
            val body = response.body ?: return@withContext Result.failure(
                Data.Builder().putString("error", "Empty response body").build()
            )

            val reader = BufferedReader(InputStreamReader(GZIPInputStream(body.byteStream())))
            val headerLine = reader.readLine() ?: return@withContext Result.failure(
                Data.Builder().putString("error", "Empty CSV file").build()
            )
            val headers = headerLine.split("\t")
            val batch = mutableListOf<ProductEntity>()
            var totalImported = 0L

            var line = reader.readLine()
            while (line != null) {
                val values = line.split("\t")
                val barcode = values.getOrElse(0) { "" }.trim()
                if (barcode.isNotBlank()) {
                    val map = HashMap<String, String>(headers.size)
                    headers.forEachIndexed { i, header -> map[header] = values.getOrElse(i) { "" } }
                    batch.add(ProductEntity(barcode = barcode, productJson = gson.toJson(map)))
                    if (batch.size >= BATCH_SIZE) {
                        repository.upsertAll(batch.toList())
                        totalImported += batch.size
                        batch.clear()
                        setProgress(
                            Data.Builder()
                                .putString("status", "Importing $totalImported rows…")
                                .build()
                        )
                    }
                }
                line = reader.readLine()
            }

            if (batch.isNotEmpty()) {
                repository.upsertAll(batch.toList())
                totalImported += batch.size
            }

            context.productDataStore.edit { prefs -> prefs[LAST_SYNC_KEY] = System.currentTimeMillis() }
            Result.success()
        } catch (e: Exception) {
            Result.failure(Data.Builder().putString("error", e.message).build())
        }
    }

    companion object {
        /** Emits the last successful sync timestamp, or null if never synced. */
        fun getLastSyncFlow(context: Context): Flow<Long?> =
            context.productDataStore.data.map { prefs -> prefs[LAST_SYNC_KEY] }

        suspend fun getLastSyncTime(context: Context): Long? =
            context.productDataStore.data.first()[LAST_SYNC_KEY]
    }
}
