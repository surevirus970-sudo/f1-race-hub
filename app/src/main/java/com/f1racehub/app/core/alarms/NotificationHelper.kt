package com.f1racehub.app.core.alarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(
    private val context: Context,
    private val notificationManager: NotificationManager? = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
) {
    companion object {
        const val CHANNEL_ID = "f1_session_reminders"
        const val CHANNEL_NAME = "F1 Заезды и Сессии"
        const val CHANNEL_DESCRIPTION = "Уведомления о старте квалификаций и гонок Формулы-1"
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(raceName: String, sessionType: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🏁 $raceName")
            .setContentText("Сессия $sessionType начнется через 15 минут!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager?.notify(raceName.hashCode(), notification)
    }
}
