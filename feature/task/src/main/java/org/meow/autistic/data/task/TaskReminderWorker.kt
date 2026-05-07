package org.meow.autistic.data.task

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.meow.autistic.core.notifications.REMINDER_CHANNEL_ID
import org.meow.autistic.feature.task.R
import java.util.concurrent.TimeUnit

/**
 * One-time worker that fires a task reminder notification at [TaskEntity.dueAt] minus
 * [TaskEntity.reminderMinutesBefore]. Keyed by task ID so updates replace pending reminders.
 */
class TaskReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: return Result.success()
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        val notification = NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_task_notification)
            .setContentTitle("Task reminder")
            .setContentText(taskTitle)
            .setAutoCancel(true)
            .build()
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(("reminder_$taskId").hashCode(), notification)
        return Result.success()
    }

    companion object {
        private const val KEY_TASK_ID = "task_id"
        private const val KEY_TASK_TITLE = "task_title"

        /** Schedules (or replaces) the reminder for [task]. No-op if no dueAt or no reminder set. */
        fun scheduleFor(workManager: WorkManager, task: TaskEntity) {
            val minutesBefore = task.reminderMinutesBefore ?: return
            val dueAt = task.dueAt ?: return
            val delayMs = dueAt.toEpochMilli() - minutesBefore * 60_000L - System.currentTimeMillis()
            if (delayMs <= 0) return

            val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_TASK_ID to task.id, KEY_TASK_TITLE to task.task))
                .build()
            workManager.enqueueUniqueWork(workName(task.id), ExistingWorkPolicy.REPLACE, request)
        }

        /** Cancels any pending reminder for [taskId]. */
        fun cancel(workManager: WorkManager, taskId: Long) {
            workManager.cancelUniqueWork(workName(taskId))
        }

        private fun workName(taskId: Long) = "task_reminder_$taskId"
    }
}
