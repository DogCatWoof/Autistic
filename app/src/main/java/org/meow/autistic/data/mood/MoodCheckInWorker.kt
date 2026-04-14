package org.meow.autistic.data.mood

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.meow.autistic.showNotification
import java.util.concurrent.TimeUnit

const val MOOD_CHECK_IN_WORK_NAME = "mood_check_in"

/**
 * Periodic worker that fires a mood check-in notification once per hour.
 * Uses [ExistingPeriodicWorkPolicy.KEEP] so app restarts don't reset the timer.
 */
class MoodCheckInWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        showNotification(applicationContext)
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                MOOD_CHECK_IN_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<MoodCheckInWorker>(1, TimeUnit.HOURS).build(),
            )
        }
    }
}
