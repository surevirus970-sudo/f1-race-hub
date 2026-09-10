# Task 2 Report: Core Time & Timezone Conversion Utilities (TDD)

## Status
DONE

## Created Files
1. `app/src/main/java/com/f1racehub/app/core/time/DateTimeFormatterUtil.kt`
   - Data class `CountdownRemaining(days: Long, hours: Long, minutes: Long, seconds: Long, isExpired: Boolean)` holding discrete breakdown components of time intervals.
   - `DateTimeFormatterUtil.parseUtcToLocalDateTime(utcIsoString: String, targetZone: ZoneId)`: parses ISO-8601 UTC strings (`Instant.parse`) and converts to target `ZoneId` returning `ZonedDateTime`.
   - `DateTimeFormatterUtil.formatToUserFriendlyTime(zonedDateTime: ZonedDateTime)`: formats zoned datetime via pattern `HH:mm, d MMMM` using the runtime default locale.
   - `DateTimeFormatterUtil.calculateRemainingDuration(targetUtc: Instant, currentUtc: Instant)`: calculates delta between current and target timestamps using `java.time.Duration`. Returns zeroed fields with `isExpired = true` for negative or zero deltas; computes days, modulo-24 hours, modulo-60 minutes, and modulo-60 seconds when positive.
2. `app/src/test/java/com/f1racehub/app/core/time/DateTimeFormatterUtilTest.kt`
   - `parseUtcToLocalDateTime should correctly adjust UTC timestamp to target timezone`: validates UTC to Europe/Moscow (+3h) offset conversion.
   - `calculateRemainingDuration should compute positive countdown and zero when past`: tests remaining delta computation and expiration on past timestamps.
   - `calculateRemainingDuration should mark exact match or zero duration as expired`: boundary condition check for delta = 0.
   - `calculateRemainingDuration should compute multi-day countdown correctly`: checks day decomposition spanning 4+ days.
   - `formatToUserFriendlyTime should format ZonedDateTime according to pattern`: validates hour, minute, and calendar day formatting output.

## Verification
- Code compilation verified via `kotlinc-jvm 2.3.10` with target JRE 25 against JDK runtime classes.
- Assertion suite executed with 100% success rate across UTC conversion, multi-day countdowns, expiration boundaries, and output formatting.
- Git commit created: `da08ed3 feat(core): add DateTimeFormatterUtil with unit tests`.

## Test Summary
5 unit test cases covering ISO-8601 parsing, timezone translation, duration decomposition, boundary expiration conditions, and locale formatting passed.

## Concerns
None. Target contracts align with Task 5 (Repository) and Task 9 (Dashboard countdown ticker).
