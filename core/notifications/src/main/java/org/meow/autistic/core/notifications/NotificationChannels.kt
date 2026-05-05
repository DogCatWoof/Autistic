package org.meow.autistic.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val MOOD_CHANNEL_ID = "mood_channel"
const val REMINDER_CHANNEL_ID = "reminders_channel"
const val SEQUENCES_CHANNEL_ID = "sequences_channel"

/** Registers all app notification channels. Call once from Application or MainActivity. */
fun registerNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.createNotificationChannel(
        NotificationChannel(MOOD_CHANNEL_ID, "Mood Check-In", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Hourly mood check-in prompts" }
    )
    nm.createNotificationChannel(
        NotificationChannel(REMINDER_CHANNEL_ID, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Reminders for upcoming tasks" }
    )
    nm.createNotificationChannel(
        NotificationChannel(SEQUENCES_CHANNEL_ID, "Sequences", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Active sequence run progress" }
    )
}
