package com.f1racehub.app.core.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SessionAlarmReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_RACE_NAME = "EXTRA_RACE_NAME"
        const val EXTRA_SESSION_TYPE = "EXTRA_SESSION_TYPE"
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"

        fun handleAlarm(
            context: Context,
            intent: Intent,
            notificationHelper: NotificationHelper = NotificationHelper(context)
        ) {
            val raceName = intent.getStringExtra(EXTRA_RACE_NAME) ?: "Гран-при Формулы-1"
            val sessionType = intent.getStringExtra(EXTRA_SESSION_TYPE) ?: "Заезд"

            notificationHelper.showReminderNotification(raceName, sessionType)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        handleAlarm(context, intent)
    }
}
