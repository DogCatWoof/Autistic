package org.meow.autistic.data.sync

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val TAG = "SyncWorker"
private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore("sync_prefs")
private val LAST_SYNC_KEY = longPreferencesKey("google_last_sync")

/**
 * Background [CoroutineWorker] that runs the full sync pipeline via [SyncOrchestrator].
 *
 * Returns [Result.retry] on any failure (no auth, network error, etc.) and
 * [Result.success] when all four steps complete without error.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val orchestrator: SyncOrchestrator by inject()

    companion object {
        /** Emits the timestamp of the last successful Google sync, or null if never synced. */
        fun getLastSyncFlow(context: Context): Flow<Long?> =
            context.syncDataStore.data.map { it[LAST_SYNC_KEY] }

        suspend fun getLastSyncTime(context: Context): Long? =
            context.syncDataStore.data.first()[LAST_SYNC_KEY]
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Sync started (attempt ${runAttemptCount + 1})")
        return when (orchestrator.sync()) {
            SyncOutcome.Success -> {
                Log.i(TAG, "Sync completed successfully")
                applicationContext.syncDataStore.edit { it[LAST_SYNC_KEY] = System.currentTimeMillis() }
                Result.success()
            }
            SyncOutcome.Retry -> {
                Log.w(TAG, "Sync deferred, will retry")
                Result.retry()
            }
        }
    }
}
