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

    @Test
    fun `calculateRemainingDuration should mark exact match or zero duration as expired`() {
        val now = Instant.parse("2026-05-24T12:00:00Z")
        val sessionStart = Instant.parse("2026-05-24T12:00:00Z")

        val diff = DateTimeFormatterUtil.calculateRemainingDuration(sessionStart, now)

        assertEquals(0, diff.days)
        assertEquals(0, diff.hours)
        assertEquals(0, diff.minutes)
        assertEquals(0, diff.seconds)
        assertTrue(diff.isExpired)
    }

    @Test
    fun `calculateRemainingDuration should compute multi-day countdown correctly`() {
        val now = Instant.parse("2026-05-20T10:00:00Z")
        val sessionStart = Instant.parse("2026-05-24T14:30:15Z")

        val diff = DateTimeFormatterUtil.calculateRemainingDuration(sessionStart, now)

        assertEquals(4, diff.days)
        assertEquals(4, diff.hours)
        assertEquals(30, diff.minutes)
        assertEquals(15, diff.seconds)
        assertFalse(diff.isExpired)
    }

    @Test
    fun `formatToUserFriendlyTime should format ZonedDateTime according to pattern`() {
        val utcIso = "2026-05-24T13:00:00Z"
        val utcZone = ZoneId.of("UTC")
        val zonedDateTime = DateTimeFormatterUtil.parseUtcToLocalDateTime(utcIso, utcZone)

        val formatted = DateTimeFormatterUtil.formatToUserFriendlyTime(zonedDateTime)

        assertTrue(formatted.startsWith("13:00, 24 "))
    }
}
