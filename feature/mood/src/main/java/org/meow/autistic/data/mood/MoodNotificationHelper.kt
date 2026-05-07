package org.meow.autistic.data.mood

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import org.meow.autistic.core.notifications.MOOD_CHANNEL_ID
import org.meow.autistic.feature.mood.R

/** Displays the mood check-in notification with emoji buttons. */
fun showMoodCheckInNotification(context: Context) {
    val views = RemoteViews(context.packageName, R.layout.notification_mood_picker)
    MOOD_EMOJIS.forEachIndexed { i, (emoji, label) ->
        views.setTextViewText(MOOD_BUTTON_VIEW_IDS[i], "$emoji\n$label")
        val intent = Intent(context, MoodBroadcastReceiver::class.java).apply {
            action = ACTION_LOG_MOOD
            putExtra(EXTRA_EMOJI, emoji)
        }
        val pi = PendingIntent.getBroadcast(
            context, i, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(MOOD_BUTTON_VIEW_IDS[i], pi)
    }
    val remoteInput = RemoteInput.Builder(REMOTE_INPUT_NOTE_KEY)
        .setLabel("Describe your activity…")
        .build()
    val noteIntent = Intent(context, MoodBroadcastReceiver::class.java).apply {
        action = ACTION_LOG_MOOD_WITH_NOTE
    }
    val notePi = PendingIntent.getBroadcast(
        context, 200, noteIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
    val noteAction = NotificationCompat.Action.Builder(0, "Add note…", notePi)
        .addRemoteInput(remoteInput)
        .build()
    val builder = NotificationCompat.Builder(context, MOOD_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_mood_notification)
        .setContentTitle("How are you feeling?")
        .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        .setCustomBigContentView(views)
        .addAction(noteAction)
    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .notify(MOOD_NOTIFICATION_ID, builder.build())
}
