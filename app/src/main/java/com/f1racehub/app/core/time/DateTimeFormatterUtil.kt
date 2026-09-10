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
