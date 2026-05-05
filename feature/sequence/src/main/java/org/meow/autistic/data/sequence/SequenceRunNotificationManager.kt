package org.meow.autistic.data.sequence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.meow.autistic.core.notifications.SEQUENCES_CHANNEL_ID
import org.meow.autistic.feature.sequence.R

const val SEQUENCE_NOTIFICATION_ID = 100

/**
 * Builds and posts the persistent sequence-run notification.
 * Re-queries the database each time so it reflects the latest progress.
 */
object SequenceRunNotificationManager {

    suspend fun update(context: Context, runId: Long, repository: SequenceRepository) {
        val run = repository.getRunById(runId) ?: return
        val sequence = repository.getById(run.sequenceId) ?: return
        val steps = repository.getStepsOnce(run.sequenceId)
        val completedIds = repository.getProgressOnce(runId).map { it.stepId }.toSet()
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
            .setSmallIcon(R.drawable.ic_sequence_notification)
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
