# Task 6 Brief: Session Alarm Infrastructure & Notifications (AlarmManager)

## Files to Create
- `app/src/main/java/com/f1racehub/app/core/alarms/NotificationHelper.kt`
- `app/src/main/java/com/f1racehub/app/core/alarms/SessionAlarmScheduler.kt`
- `app/src/main/java/com/f1racehub/app/core/alarms/SessionAlarmReceiver.kt`
- `app/src/main/java/com/f1racehub/app/core/alarms/BootCompletedReceiver.kt`
- `app/src/test/java/com/f1racehub/app/core/alarms/SessionAlarmSchedulerTest.kt`

## Interfaces
- Consumes: `android.app.AlarmManager`, `android.app.NotificationManager`, `PendingIntent`, `Context`.
- Produces:
  - `NotificationHelper(context: Context)`:
    - `showReminderNotification(raceName: String, sessionType: String)`
  - `SessionAlarmScheduler(context: Context)`:
    - `fun scheduleAlarm(sessionId: String, raceName: String, sessionType: String, triggerTimeMillis: Long)`
    - `fun cancelAlarm(sessionId: String)`
  - `SessionAlarmReceiver : BroadcastReceiver()`
  - `BootCompletedReceiver : BroadcastReceiver()`

## Implementation Details

### 1. `NotificationHelper.kt`
```kotlin
package com.f1racehub.app.core.alarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "f1_session_reminders"
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "F1 Заезды и Сессии",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о старте квалификаций и гонок Формулы-1"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
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

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(raceName.hashCode(), notification)
    }
}
```

### 2. `SessionAlarmScheduler.kt`
```kotlin
package com.f1racehub.app.core.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class SessionAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleAlarm(sessionId: String, raceName: String, sessionType: String, triggerTimeMillis: Long) {
        val intent = Intent(context, SessionAlarmReceiver::class.java).apply {
            putExtra("EXTRA_RACE_NAME", raceName)
            putExtra("EXTRA_SESSION_TYPE", sessionType)
            putExtra("EXTRA_SESSION_ID", sessionId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )
    }

    fun cancelAlarm(sessionId: String) {
        val intent = Intent(context, SessionAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
    }
}
```

### 3. `SessionAlarmReceiver.kt` & `BootCompletedReceiver.kt`
```kotlin
package com.f1racehub.app.core.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SessionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val raceName = intent.getStringExtra("EXTRA_RACE_NAME") ?: "Гран-при Формулы-1"
        val sessionType = intent.getStringExtra("EXTRA_SESSION_TYPE") ?: "Заезд"

        val helper = NotificationHelper(context)
        helper.showReminderNotification(raceName, sessionType)
    }
}
```

```kotlin
package com.f1racehub.app.core.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Регистрация будет переинициализирована при старте приложения через Room DAO
        }
    }
}
```

### 4. Unit Test `SessionAlarmSchedulerTest.kt`
Unit test mocking Context and AlarmManager:
- Verify `scheduleAlarm` calls `setExactAndAllowWhileIdle` with RTC_WAKEUP and expected triggerTime.
- Verify `cancelAlarm` calls `alarmManager.cancel`.
- Validate Intent extras and PendingIntent flags (`FLAG_IMMUTABLE`).

### 5. Commit
Commit with message: `feat(alarms): implement AlarmManager scheduler and notification receiver`
