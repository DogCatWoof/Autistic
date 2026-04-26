package org.meow.autistic.data.sequence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.meow.autistic.R
import org.meow.autistic.data.task.TaskDatabase

const val SEQUENCE_NOTIFICATION_ID = 100
const val SEQUENCES_CHANNEL_ID = "sequences_channel"

/**
 * Builds and posts the persistent sequence-run notification.
 * Re-queries the database each time so it reflects the latest progress.
 */
object SequenceRunNotificationManager {

    suspend fun update(context: Context, runId: Long) {
        val db = TaskDatabase.getDatabase(context)
        val run = db.sequenceDao().getRunById(runId) ?: return
        val sequence = db.sequenceDao().getById(run.sequenceId) ?: return
        val steps = db.sequenceDao().getStepsOnce(run.sequenceId)
        val completedIds = db.sequenceDao().getProgressOnce(runId).map { it.stepId }.toSet()
        val currentStep = steps.firstOrNull { it.id !in completedIds } ?: return

        val stepIndex = steps.indexOfFirst { it.id == currentStep.id } + 1
        val completeIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, SequenceStepReceiver::class.java).apply {
                action = ACTION_COMPLETE_STEP
                putExtra(EXTRA_RUN_ID, runId)
                putExtra(EXTRA_STEP_ID, currentStep.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val endIntent = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, SequenceStepReceiver::class.java).apply {
                action = ACTION_END_RUN
                putExtra(EXTRA_RUN_ID, runId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, SEQUENCES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(sequence.name)
            .setContentText("Step $stepIndex of ${steps.size}: ${currentStep.instruction}")
            .setOngoing(true)
            .addAction(0, "Done", completeIntent)
            .addAction(0, "End", endIntent)
            .build()
        NotificationManagerCompat.from(context).notify(SEQUENCE_NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(SEQUENCE_NOTIFICATION_ID)
    }
}
