package org.meow.autistic.data.task

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.meow.autistic.data.foodlog.FoodLogDao
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val DAILY_RESET_WORK_NAME = "daily_reset"

private val Context.dailyResetDataStore: DataStore<Preferences>
    by preferencesDataStore("daily_reset_prefs")

private val LAST_RESET_DATE_KEY = stringPreferencesKey("last_reset_date")

/**
 * One-time worker enqueued on every app start.
 *
 * If today's date differs from the stored last-reset date it:
 * 1. Deletes all incomplete [TaskEntity] rows that were generated from daily tasks.
 * 2. Re-inserts each [DailyTaskEntity] as a new [TaskEntity] due today at its stored time.
 *
 * Uses [ExistingWorkPolicy.KEEP] so rapid app restarts don't run it twice.
 */
class DailyResetWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val taskDao: TaskDao by inject()
    private val dailyTaskRepository: DailyTaskRepository by inject()
    private val foodLogDao: FoodLogDao by inject()

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val prefs = context.dailyResetDataStore.data.first()
        if (prefs[LAST_RESET_DATE_KEY] == todayStr) return Result.success()

        foodLogDao.deleteOlderThan(today.minusDays(14).toString())

        taskDao.deleteUnfinishedDailyTasks()

        val todayStartMs = today
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val now = Instant.now()
        dailyTaskRepository.getAllOnce().forEach { dailyTask ->
            val timeMinutes = dailyTask.timeMinutes
            val dueAtMs = if (timeMinutes != null) {
                todayStartMs + timeMinutes * 60_000L
            } else {
                todayStartMs
            }
            taskDao.insertTask(
                TaskEntity(
                    task = dailyTask.title,
                    category = dailyTask.category,
                    dueAt = Instant.ofEpochMilli(dueAtMs),
                    createdAt = now,
                    dailyTaskId = dailyTask.id,
                    syncStatus = "local",
                    expectedTimeMinutes = dailyTask.expectedTimeMinutes,
                    isImportant = dailyTask.isRequired,
                )
            )
        }

        context.dailyResetDataStore.edit { it[LAST_RESET_DATE_KEY] = todayStr }
        return Result.success()
    }

    companion object {
        /** Emits the date string of the last daily reset ("yyyy-MM-dd"), or null if never run. */
        fun getLastResetFlow(context: Context): Flow<String?> =
            context.dailyResetDataStore.data.map { it[LAST_RESET_DATE_KEY] }

        /** Enqueues the worker; KEEP policy prevents double-runs on rapid restarts. */
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                DAILY_RESET_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<DailyResetWorker>().build(),
            )
        }

        /**
         * Clears the stored last-reset date then re-enqueues with REPLACE,
         * forcing a full reset even if one already ran today.
         */
        suspend fun forceReset(context: Context) {
            context.dailyResetDataStore.edit { it.remove(LAST_RESET_DATE_KEY) }
            WorkManager.getInstance(context).enqueueUniqueWork(
                DAILY_RESET_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<DailyResetWorker>().build(),
            )
        }
    }
}
