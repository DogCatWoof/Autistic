package org.meow.autistic.data.todo

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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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
 * 1. Deletes all incomplete [TodoEntity] rows that were generated from daily tasks.
 * 2. Re-inserts each [DailyTaskEntity] as a new [TodoEntity] due today at its stored time.
 *
 * Uses [ExistingWorkPolicy.KEEP] so rapid app restarts don't run it twice.
 */
class DailyResetWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val todoDao: TodoDao by inject()
    private val dailyTaskRepository: DailyTaskRepository by inject()

    override suspend fun doWork(): Result {
        val today = LocalDate.now().toString()
        val prefs = context.dailyResetDataStore.data.first()
        if (prefs[LAST_RESET_DATE_KEY] == today) return Result.success()

        todoDao.deleteUnfinishedDailyTodos()

        val todayStartMs = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val now = System.currentTimeMillis()
        dailyTaskRepository.getAllOnce().forEach { dailyTask ->
            val dueAt = if (dailyTask.timeMinutes != null) {
                todayStartMs + dailyTask.timeMinutes * 60_000L
            } else {
                todayStartMs
            }
            todoDao.insertTodo(
                TodoEntity(
                    task = dailyTask.title,
                    category = dailyTask.category,
                    dueAt = dueAt,
                    createdAt = now,
                    dailyTaskId = dailyTask.id,
                    syncStatus = "local",
                )
            )
        }

        context.dailyResetDataStore.edit { it[LAST_RESET_DATE_KEY] = today }
        return Result.success()
    }

    companion object {
        /** Enqueues the worker; KEEP policy prevents double-runs on rapid restarts. */
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                DAILY_RESET_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<DailyResetWorker>().build(),
            )
        }
    }
}
