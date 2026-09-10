package com.f1racehub.app.core.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class SessionAlarmScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager? = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager,
    private val intentFactory: (Context, Class<*>) -> Intent = { ctx, cls -> Intent(ctx, cls) },
    private val pendingIntentFactory: (Context, Int, Intent, Int) -> PendingIntent? = { ctx, requestCode, intent, flags ->
        PendingIntent.getBroadcast(ctx, requestCode, intent, flags)
    }
) {
    fun scheduleAlarm(sessionId: String, raceName: String, sessionType: String, triggerTimeMillis: Long) {
        val intent = intentFactory(context, SessionAlarmReceiver::class.java).apply {
            putExtra(SessionAlarmReceiver.EXTRA_RACE_NAME, raceName)
            putExtra(SessionAlarmReceiver.EXTRA_SESSION_TYPE, sessionType)
            putExtra(SessionAlarmReceiver.EXTRA_SESSION_ID, sessionId)
        }
        val pendingIntent = pendingIntentFactory(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(sessionId: String) {
        val intent = intentFactory(context, SessionAlarmReceiver::class.java)
        val pendingIntent = pendingIntentFactory(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
        }
    }
}
