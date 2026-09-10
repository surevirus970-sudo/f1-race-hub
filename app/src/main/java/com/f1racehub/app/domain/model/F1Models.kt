package com.f1racehub.app.domain.model

import java.time.ZonedDateTime

data class GrandPrix(
    val round: Int,
    val name: String,
    val circuitName: String,
    val country: String,
    val locality: String,
    val sessions: List<RaceSession>,
    val isCompleted: Boolean
)

data class RaceSession(
    val id: String,
    val type: SessionType,
    val startTime: ZonedDateTime,
    val isAlarmSet: Boolean
)

enum class SessionType {
    PRACTICE_1, PRACTICE_2, PRACTICE_3, QUALIFYING, SPRINT, RACE
}
