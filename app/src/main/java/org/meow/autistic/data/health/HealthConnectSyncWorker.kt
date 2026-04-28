package org.meow.autistic.data.health

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

internal const val HEALTH_SYNC_WORK_NAME = "health_connect_sync"

/**
 * Reads each Health Connect data type and writes a [HealthSnapshotEntity] for today.
 * Runs once per hour on any network. No-ops silently when Health Connect is unavailable
 * or permissions are not granted.
 */
class HealthConnectSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: HealthConnectRepository by inject()

    override suspend fun doWork(): Result {
        repository.refreshTodaySnapshot()
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<HealthConnectSyncWorker>(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                HEALTH_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
