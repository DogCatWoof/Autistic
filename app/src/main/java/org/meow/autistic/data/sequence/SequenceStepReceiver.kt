package org.meow.autistic.data.sequence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.meow.autistic.data.task.TaskDatabase
import java.time.Instant

const val ACTION_COMPLETE_STEP = "org.meow.autistic.ACTION_COMPLETE_STEP"
const val ACTION_END_RUN = "org.meow.autistic.ACTION_END_RUN"
const val EXTRA_RUN_ID = "extra_run_id"
const val EXTRA_STEP_ID = "extra_step_id"

/**
 * Handles step-completion and run-end actions fired from the persistent sequence notification.
 */
class SequenceStepReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val runId = intent.getLongExtra(EXTRA_RUN_ID, -1L).takeIf { it >= 0 } ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = TaskDatabase.getDatabase(context).sequenceDao()
                when (intent.action) {
                    ACTION_COMPLETE_STEP -> {
                        val stepId = intent.getLongExtra(EXTRA_STEP_ID, -1L).takeIf { it >= 0 }
                            ?: return@launch
                        dao.upsertProgress(SequenceStepProgressEntity(runId, stepId, Instant.now()))
                        val run = dao.getRunById(runId) ?: return@launch
                        val steps = dao.getStepsOnce(run.sequenceId)
                        val completed = dao.getProgressOnce(runId)
                        if (completed.size >= steps.size) {
                            dao.updateRun(run.copy(completedAt = Instant.now()))
                            NotificationManagerCompat.from(context).cancel(SEQUENCE_NOTIFICATION_ID)
                        } else {
                            SequenceRunNotificationManager.update(context, runId)
                        }
                    }
                    ACTION_END_RUN -> {
                        val run = dao.getRunById(runId) ?: return@launch
                        dao.updateRun(run.copy(completedAt = Instant.now()))
                        NotificationManagerCompat.from(context).cancel(SEQUENCE_NOTIFICATION_ID)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
