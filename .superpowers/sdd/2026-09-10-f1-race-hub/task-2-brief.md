# Task 2 Brief: Core Time & Timezone Conversion Utilities (TDD)

## Files to Create
- Test: `app/src/test/java/com/f1racehub/app/core/time/DateTimeFormatterUtilTest.kt`
- Source: `app/src/main/java/com/f1racehub/app/core/time/DateTimeFormatterUtil.kt`

## Interfaces
- Consumes: Java Time API (`Instant`, `ZonedDateTime`, `ZoneId`, `Duration`).
- Produces:
  - `data class CountdownRemaining(val days: Long, val hours: Long, val minutes: Long, val seconds: Long, val isExpired: Boolean)`
  - `DateTimeFormatterUtil.parseUtcToLocalDateTime(utcIsoString: String, targetZone: ZoneId = ZoneId.systemDefault()): ZonedDateTime`
  - `DateTimeFormatterUtil.formatToUserFriendlyTime(zonedDateTime: ZonedDateTime): String`
  - `DateTimeFormatterUtil.calculateRemainingDuration(targetUtc: Instant, currentUtc: Instant = Instant.now()): CountdownRemaining`

## TDD Implementation Steps

### Step 1: Write Failing Unit Test
File: `app/src/test/java/com/f1racehub/app/core/time/DateTimeFormatterUtilTest.kt`
```kotlin
package com.f1racehub.app.core.time

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId

class DateTimeFormatterUtilTest {

    @Test
    fun `parseUtcToLocalDateTime should correctly adjust UTC timestamp to target timezone`() {
        val utcIso = "2026-05-24T13:00:00Z"
        val moscowZone = ZoneId.of("Europe/Moscow") // UTC+3

        val result = DateTimeFormatterUtil.parseUtcToLocalDateTime(utcIso, moscowZone)

        assertEquals(2026, result.year)
        assertEquals(5, result.monthValue)
        assertEquals(24, result.dayOfMonth)
        assertEquals(16, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun `calculateRemainingDuration should compute positive countdown and zero when past`() {
        val now = Instant.parse("2026-05-24T12:00:00Z")
        val sessionStart = Instant.parse("2026-05-24T14:30:15Z")

        val diff = DateTimeFormatterUtil.calculateRemainingDuration(sessionStart, now)

        assertEquals(0, diff.days)
        assertEquals(2, diff.hours)
        assertEquals(30, diff.minutes)
        assertEquals(15, diff.seconds)
        assertFalse(diff.isExpired)

        val pastSession = Instant.parse("2026-05-24T10:00:00Z")
        val pastDiff = DateTimeFormatterUtil.calculateRemainingDuration(pastSession, now)
        assertTrue(pastDiff.isExpired)
    }
}
```

### Step 2: Implement Code
File: `app/src/main/java/com/f1racehub/app/core/time/DateTimeFormatterUtil.kt`
```kotlin
package com.f1racehub.app.core.time

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CountdownRemaining(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val isExpired: Boolean
)

object DateTimeFormatterUtil {

    fun parseUtcToLocalDateTime(utcIsoString: String, targetZone: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
        val instant = Instant.parse(utcIsoString)
        return instant.atZone(targetZone)
    }

    fun formatToUserFriendlyTime(zonedDateTime: ZonedDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm, d MMMM", Locale.getDefault())
        return zonedDateTime.format(formatter)
    }

    fun calculateRemainingDuration(targetUtc: Instant, currentUtc: Instant = Instant.now()): CountdownRemaining {
        val duration = Duration.between(currentUtc, targetUtc)
        if (duration.isNegative || duration.isZero) {
            return CountdownRemaining(0, 0, 0, 0, isExpired = true)
        }
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60
        return CountdownRemaining(days, hours, minutes, seconds, isExpired = false)
    }
}
```

### Step 3: Verify and Commit
Commit with message: `feat(core): add DateTimeFormatterUtil with unit tests`
