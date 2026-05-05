package org.meow.autistic.data.mood

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.meow.autistic.feature.mood.R
import java.time.Instant

const val MOOD_NOTIFICATION_ID = 1
const val ACTION_LOG_MOOD = "org.meow.autistic.ACTION_LOG_MOOD"
const val ACTION_LOG_MOOD_WITH_NOTE = "org.meow.autistic.ACTION_LOG_MOOD_WITH_NOTE"
const val EXTRA_EMOJI = "extra_emoji"
const val REMOTE_INPUT_NOTE_KEY = "mood_note_input"

val MOOD_BUTTON_VIEW_IDS = intArrayOf(
    R.id.mood_btn_0,
    R.id.mood_btn_1,
    R.id.mood_btn_2,
    R.id.mood_btn_3,
    R.id.mood_btn_4,
    R.id.mood_btn_5,
    R.id.mood_btn_6,
    R.id.mood_btn_7,
    R.id.mood_btn_8,
    R.id.mood_btn_9,
    R.id.mood_btn_10,
    R.id.mood_btn_11,
    R.id.mood_btn_12,
    R.id.mood_btn_13,
)

/**
 * Receives mood selection intents fired from notification buttons.
 * Saves the chosen emoji to the database and cancels the notification.
 */
class MoodBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    private val moodDao: MoodDao by inject()

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
            ?.trim()?.takeIf { it.isNotBlank() } ?: return
        saveAndDismiss(context, "", note)
    }

    private fun saveAndDismiss(context: Context, emoji: String, note: String) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                moodDao.insert(MoodEntity(emoji = emoji, activity = note, createdAt = Instant.now()))
            } finally {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(MOOD_NOTIFICATION_ID)
                pendingResult.finish()
            }
        }
    }
}
