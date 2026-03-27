package org.meow.autistic.data.sync

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal const val WIFI_WORK_NAME = "sync_wifi"
internal const val CELLULAR_WORK_NAME = "sync_cellular"
internal const val IMMEDIATE_WORK_NAME = "sync_immediate"

private const val WIFI_INTERVAL_MINUTES = 15L
private const val CELLULAR_INTERVAL_HOURS = 1L

/**
 * Schedules [SyncWorker] as periodic background work and triggers immediate one-time syncs.
 *
 * Two periodic workers run concurrently:
 * - **WiFi** (15 min, [NetworkType.UNMETERED]) — frequent sync on unmetered connections.
 * - **Cellular** (1 hour, [NetworkType.CONNECTED]) — fallback sync on any network when not on WiFi.
 *
 * @param workManager WorkManager instance; pass [WorkManager.getInstance] from the call site.
 */
class SyncScheduler(private val workManager: WorkManager) {

    /**
     * Enqueues both periodic sync workers, replacing any existing workers with the same name.
     * Safe to call multiple times (e.g. on every app start).
     */
    fun schedulePeriodicSync() {
        val wifiRequest = PeriodicWorkRequestBuilder<SyncWorker>(WIFI_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build(),
            )
            .build()
        workManager.enqueueUniquePeriodicWork(WIFI_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, wifiRequest)

        val cellularRequest = PeriodicWorkRequestBuilder<SyncWorker>(CELLULAR_INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        workManager.enqueueUniquePeriodicWork(CELLULAR_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, cellularRequest)
    }

    /**
     * Enqueues a one-time immediate sync, replacing any pending immediate sync that hasn't started.
     * Called by the manual sync button in the UI.
     */
    fun triggerImmediate() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
        workManager.enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
