package kz.kimep.mobile.data.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

object Notifications {
    const val CHANNEL_LESSONS = "lessons"
    const val CHANNEL_FINALS = "finals"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val lessons = NotificationChannel(
            CHANNEL_LESSONS,
            "Class reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminds you 1 hour 10 minutes before a class starts"
        }
        val finals = NotificationChannel(
            CHANNEL_FINALS,
            "Final exam reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminds you the day before a final exam"
        }
        manager.createNotificationChannels(listOf(lessons, finals))
    }
}
