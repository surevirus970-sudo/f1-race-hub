package com.f1racehub.app.core.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SessionAlarmSchedulerTest {

    private lateinit var mockContext: Context
    private lateinit var mockAlarmManager: AlarmManager
    private lateinit var mockIntent: Intent
    private lateinit var mockPendingIntent: PendingIntent

    private var capturedRequestCode: Int? = null
    private var capturedFlags: Int? = null
    private var capturedTargetClass: Class<*>? = null

    @BeforeEach
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockAlarmManager = mockk(relaxed = true)
        mockIntent = mockk(relaxed = true)
        mockPendingIntent = mockk(relaxed = true)

        capturedRequestCode = null
        capturedFlags = null
        capturedTargetClass = null

        every { mockContext.getSystemService(Context.ALARM_SERVICE) } returns mockAlarmManager
    }

    private fun createScheduler(customAlarmManager: AlarmManager? = mockAlarmManager): SessionAlarmScheduler {
        return SessionAlarmScheduler(
            context = mockContext,
            alarmManager = customAlarmManager,
            intentFactory = { _, targetClass ->
                capturedTargetClass = targetClass
                mockIntent
            },
            pendingIntentFactory = { _, requestCode, _, flags ->
                capturedRequestCode = requestCode
                capturedFlags = flags
                mockPendingIntent
            }
        )
    }

    @Test
    fun `scheduleAlarm should set exact alarm with RTC_WAKEUP and expected trigger time`() {
        val scheduler = createScheduler()
        val triggerTime = 1774000000000L

        scheduler.scheduleAlarm(
            sessionId = "1_QUALIFYING",
            raceName = "Bahrain Grand Prix",
            sessionType = "QUALIFYING",
            triggerTimeMillis = triggerTime
        )

        verify(exactly = 1) {
            mockAlarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                mockPendingIntent
            )
        }
    }

    @Test
    fun `scheduleAlarm should configure intent extras with raceName sessionType and sessionId`() {
        val scheduler = createScheduler()
        val sessionId = "1_RACE"
        val raceName = "Bahrain Grand Prix"
        val sessionType = "RACE"

        scheduler.scheduleAlarm(
            sessionId = sessionId,
            raceName = raceName,
            sessionType = sessionType,
            triggerTimeMillis = 1774000000000L
        )

        assertEquals(SessionAlarmReceiver::class.java, capturedTargetClass)
        verify(exactly = 1) { mockIntent.putExtra(SessionAlarmReceiver.EXTRA_RACE_NAME, raceName) }
        verify(exactly = 1) { mockIntent.putExtra(SessionAlarmReceiver.EXTRA_SESSION_TYPE, sessionType) }
        verify(exactly = 1) { mockIntent.putExtra(SessionAlarmReceiver.EXTRA_SESSION_ID, sessionId) }
    }

    @Test
    fun `scheduleAlarm should generate PendingIntent with sessionId hashCode and immutable flags`() {
        val scheduler = createScheduler()
        val sessionId = "2_SPRINT"

        scheduler.scheduleAlarm(
            sessionId = sessionId,
            raceName = "Chinese Grand Prix",
            sessionType = "SPRINT",
            triggerTimeMillis = 1775000000000L
        )

        assertEquals(sessionId.hashCode(), capturedRequestCode)
        val expectedFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        assertEquals(expectedFlags, capturedFlags)
        assertTrue(
            (capturedFlags!! and PendingIntent.FLAG_IMMUTABLE) != 0,
            "FLAG_IMMUTABLE must be included in PendingIntent flags"
        )
    }

    @Test
    fun `scheduleAlarm should safely handle null AlarmManager without throwing exception`() {
        val scheduler = createScheduler(customAlarmManager = null)

        scheduler.scheduleAlarm(
            sessionId = "1_FP1",
            raceName = "Bahrain Grand Prix",
            sessionType = "FP1",
            triggerTimeMillis = 1774000000000L
        )

        verify(exactly = 0) { mockAlarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
    }

    @Test
    fun `cancelAlarm should cancel PendingIntent on AlarmManager with sessionId hashCode`() {
        val scheduler = createScheduler()
        val sessionId = "1_QUALIFYING"

        scheduler.cancelAlarm(sessionId = sessionId)

        assertEquals(sessionId.hashCode(), capturedRequestCode)
        val expectedFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        assertEquals(expectedFlags, capturedFlags)
        assertEquals(SessionAlarmReceiver::class.java, capturedTargetClass)

        verify(exactly = 1) { mockAlarmManager.cancel(mockPendingIntent) }
    }

    @Test
    fun `cancelAlarm should safely handle null AlarmManager without throwing exception`() {
        val scheduler = createScheduler(customAlarmManager = null)

        scheduler.cancelAlarm(sessionId = "1_QUALIFYING")

        verify(exactly = 0) { mockAlarmManager.cancel(any<PendingIntent>()) }
    }

    @Test
    fun `SessionAlarmReceiver handleAlarm should extract extras and invoke helper notification`() {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.getStringExtra(SessionAlarmReceiver.EXTRA_RACE_NAME) } returns "Monaco Grand Prix"
        every { intent.getStringExtra(SessionAlarmReceiver.EXTRA_SESSION_TYPE) } returns "QUALIFYING"

        var reminderRaceName: String? = null
        var reminderSessionType: String? = null

        val helperMock = mockk<NotificationHelper>(relaxed = true)
        every { helperMock.showReminderNotification(any(), any()) } answers {
            reminderRaceName = firstArg()
            reminderSessionType = secondArg()
        }

        SessionAlarmReceiver.handleAlarm(mockContext, intent, helperMock)

        assertEquals("Monaco Grand Prix", reminderRaceName)
        assertEquals("QUALIFYING", reminderSessionType)
    }

    @Test
    fun `SessionAlarmReceiver handleAlarm should fallback to default strings when extras are absent`() {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.getStringExtra(any()) } returns null

        var reminderRaceName: String? = null
        var reminderSessionType: String? = null

        val helperMock = mockk<NotificationHelper>(relaxed = true)
        every { helperMock.showReminderNotification(any(), any()) } answers {
            reminderRaceName = firstArg()
            reminderSessionType = secondArg()
        }

        SessionAlarmReceiver.handleAlarm(mockContext, intent, helperMock)

        assertEquals("Гран-при Формулы-1", reminderRaceName)
        assertEquals("Заезд", reminderSessionType)
    }

    @Test
    fun `BootCompletedReceiver handleBootCompleted handles ACTION_BOOT_COMPLETED and other actions safely`() {
        val bootIntent = mockk<Intent>(relaxed = true)
        every { bootIntent.action } returns Intent.ACTION_BOOT_COMPLETED

        BootCompletedReceiver.handleBootCompleted(mockContext, bootIntent)

        val otherIntent = mockk<Intent>(relaxed = true)
        every { otherIntent.action } returns "android.intent.action.OTHER"

        BootCompletedReceiver.handleBootCompleted(mockContext, otherIntent)
    }

    @Test
    fun `NotificationHelper constants and keys are valid`() {
        assertEquals("f1_session_reminders", NotificationHelper.CHANNEL_ID)
        assertEquals("EXTRA_RACE_NAME", SessionAlarmReceiver.EXTRA_RACE_NAME)
        assertEquals("EXTRA_SESSION_TYPE", SessionAlarmReceiver.EXTRA_SESSION_TYPE)
        assertEquals("EXTRA_SESSION_ID", SessionAlarmReceiver.EXTRA_SESSION_ID)
    }
}
