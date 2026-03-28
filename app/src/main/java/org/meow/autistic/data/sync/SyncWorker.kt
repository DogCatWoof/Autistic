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
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.auth.TokenStore
import org.meow.autistic.data.calendar.CalendarRemoteSource
import org.meow.autistic.data.calendar.CalendarRepository
import org.meow.autistic.data.calendar.CalendarSyncService
import org.meow.autistic.data.calendar.CalendarSyncTokenStore
import org.meow.autistic.data.todo.GoogleTasksRemoteSource
import org.meow.autistic.data.todo.GoogleTasksSyncService
import org.meow.autistic.data.todo.TodoDatabase
import org.meow.autistic.data.todo.TodoRepository

private const val TAG = "SyncWorker"
private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore("sync_prefs")
private val LAST_SYNC_KEY = longPreferencesKey("google_last_sync")

/**
 * Background [CoroutineWorker] that runs the full sync pipeline via [SyncOrchestrator].
 *
 * Returns [Result.retry] on any failure (no auth, network error, etc.) and
 * [Result.success] when all four steps complete without error.
 *
 * Dependencies are constructed from [applicationContext] on first run, matching the
 * pattern used elsewhere in the app (no DI framework required).
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private val orchestrator: SyncOrchestrator

    init {
        val tokenStore = TokenStore.create(appContext)
        val authManager = GoogleAuthManager(appContext, tokenStore)
        val db = TodoDatabase.getDatabase(appContext)
        val tasksSyncService = GoogleTasksSyncService(
            remoteSource = GoogleTasksRemoteSource(),
            repository = TodoRepository(db.todoDao()),
            tokenProvider = { authManager.getValidToken() },
        )
        val calendarSyncService = CalendarSyncService(
            remoteSource = CalendarRemoteSource(),
            repository = CalendarRepository(db.calendarDao()),
            syncTokenStore = CalendarSyncTokenStore(appContext),
            tokenProvider = { authManager.getValidToken() },
        )
        orchestrator = SyncOrchestrator(authManager, tasksSyncService, calendarSyncService)
    }

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
