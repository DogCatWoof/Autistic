package org.meow.autistic.data.mood

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.meow.autistic.data.task.TaskDatabase
import java.time.Instant

const val MOOD_NOTIFICATION_ID = 1
const val ACTION_LOG_MOOD = "org.meow.autistic.ACTION_LOG_MOOD"
const val ACTION_LOG_MOOD_WITH_NOTE = "org.meow.autistic.ACTION_LOG_MOOD_WITH_NOTE"
const val EXTRA_EMOJI = "extra_emoji"
const val REMOTE_INPUT_NOTE_KEY = "mood_note_input"

val MOOD_BUTTON_VIEW_IDS = intArrayOf(
    org.meow.autistic.R.id.mood_btn_0,
    org.meow.autistic.R.id.mood_btn_1,
    org.meow.autistic.R.id.mood_btn_2,
    org.meow.autistic.R.id.mood_btn_3,
    org.meow.autistic.R.id.mood_btn_4,
    org.meow.autistic.R.id.mood_btn_5,
    org.meow.autistic.R.id.mood_btn_6,
    org.meow.autistic.R.id.mood_btn_7,
    org.meow.autistic.R.id.mood_btn_8,
    org.meow.autistic.R.id.mood_btn_9,
    org.meow.autistic.R.id.mood_btn_10,
    org.meow.autistic.R.id.mood_btn_11,
    org.meow.autistic.R.id.mood_btn_12,
    org.meow.autistic.R.id.mood_btn_13,
)

/**
 * Receives mood selection intents fired from notification buttons.
 * Saves the chosen emoji to the database and cancels the notification.
 */
class MoodBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_LOG_MOOD -> handleQuickLog(context, intent)
            ACTION_LOG_MOOD_WITH_NOTE -> handleLogWithNote(context, intent)
        }
    }

    private fun handleQuickLog(context: Context, intent: Intent) {
        val emoji = intent.getStringExtra(EXTRA_EMOJI) ?: return
        saveAndDismiss(context, emoji, "")
    }

    private fun handleLogWithNote(context: Context, intent: Intent) {
        val note = RemoteInput.getResultsFromIntent(intent)?.getString(REMOTE_INPUT_NOTE_KEY)
            ?.trim() ?: return
        if (note.isBlank()) { return }
        saveAndDismiss(context, "", note)
    }

    private fun saveAndDismiss(context: Context, emoji: String, note: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TaskDatabase.getDatabase(context).moodDao()
                    .insert(MoodEntity(emoji = emoji, activity = note, createdAt = Instant.now()))
            } finally {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(MOOD_NOTIFICATION_ID)
                pendingResult.finish()
            }
        }
    }
}
