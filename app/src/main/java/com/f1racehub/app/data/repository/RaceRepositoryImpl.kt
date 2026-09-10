package com.f1racehub.app.data.repository

import com.f1racehub.app.core.time.DateTimeFormatterUtil
import com.f1racehub.app.data.local.dao.RaceDao
import com.f1racehub.app.data.local.entities.RaceEntity
import com.f1racehub.app.data.local.entities.RaceSessionEntity
import com.f1racehub.app.data.remote.F1ApiClient
import com.f1racehub.app.domain.model.GrandPrix
import com.f1racehub.app.domain.model.RaceSession
import com.f1racehub.app.domain.model.SessionType
import com.f1racehub.app.domain.repository.RaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RaceRepositoryImpl(
    private val raceDao: RaceDao,
    private val apiClient: F1ApiClient
) : RaceRepository {

    override fun observeGrandPrixList(): Flow<List<GrandPrix>> {
        return raceDao.observeRacesWithSessions().map { list ->
            list.map { raceWithSessions ->
                GrandPrix(
                    round = raceWithSessions.race.round,
                    name = raceWithSessions.race.raceName,
                    circuitName = raceWithSessions.race.circuitName,
                    country = raceWithSessions.race.country,
                    locality = raceWithSessions.race.locality,
                    isCompleted = raceWithSessions.race.isCompleted,
                    sessions = raceWithSessions.sessions.map { s ->
                        RaceSession(
                            id = s.sessionId,
                            type = mapSessionType(s.sessionType),
                            startTime = DateTimeFormatterUtil.parseUtcToLocalDateTime(s.startTimeUtc),
                            isAlarmSet = s.isAlarmSet
                        )
                    }
                )
            }
        }
    }

    override suspend fun refreshSchedule(): Result<Unit> = runCatching {
        val existingAlarms = raceDao.getScheduledAlarms().map { it.sessionId }.toSet()
        val dtos = apiClient.fetchCurrentSeasonRaces()
        val raceEntities = dtos.map {
            RaceEntity(
                round = it.round.toInt(),
                raceName = it.raceName,
                circuitId = it.circuit.circuitId,
                circuitName = it.circuit.circuitName,
                country = it.circuit.location.country,
                locality = it.circuit.location.locality
            )
        }
        val sessionEntities = mutableListOf<RaceSessionEntity>()
        dtos.forEach { r ->
            val round = r.round.toInt()
            r.firstPractice?.let { fp1 ->
                val id = "${round}_FP1"
                sessionEntities.add(
                    RaceSessionEntity(
                        sessionId = id,
                        round = round,
                        sessionType = "FP1",
                        startTimeUtc = formatTimestamp(fp1.date, fp1.time, "10:30:00Z"),
                        isAlarmSet = existingAlarms.contains(id)
                    )
                )
            }
            r.secondPractice?.let { fp2 ->
                val id = "${round}_FP2"
                sessionEntities.add(
                    RaceSessionEntity(
                        sessionId = id,
                        round = round,
                        sessionType = "FP2",
                        startTimeUtc = formatTimestamp(fp2.date, fp2.time, "14:00:00Z"),
                        isAlarmSet = existingAlarms.contains(id)
                    )
                )
            }
            r.thirdPractice?.let { fp3 ->
                val id = "${round}_FP3"
                sessionEntities.add(
                    RaceSessionEntity(
                        sessionId = id,
                        round = round,
                        sessionType = "FP3",
                        startTimeUtc = formatTimestamp(fp3.date, fp3.time, "11:30:00Z"),
                        isAlarmSet = existingAlarms.contains(id)
                    )
                )
            }
            r.qualifying?.let { q ->
                val id = "${round}_QUALIFYING"
                sessionEntities.add(
                    RaceSessionEntity(
                        sessionId = id,
                        round = round,
                        sessionType = "QUALIFYING",
                        startTimeUtc = formatTimestamp(q.date, q.time, "15:00:00Z"),
                        isAlarmSet = existingAlarms.contains(id)
                    )
                )
            }
            r.sprint?.let { sp ->
                val id = "${round}_SPRINT"
                sessionEntities.add(
                    RaceSessionEntity(
                        sessionId = id,
                        round = round,
                        sessionType = "SPRINT",
                        startTimeUtc = formatTimestamp(sp.date, sp.time, "12:00:00Z"),
                        isAlarmSet = existingAlarms.contains(id)
                    )
                )
            }
            val raceId = "${round}_RACE"
            sessionEntities.add(
                RaceSessionEntity(
                    sessionId = raceId,
                    round = round,
                    sessionType = "RACE",
                    startTimeUtc = formatTimestamp(r.date, r.time, "13:00:00Z"),
                    isAlarmSet = existingAlarms.contains(raceId)
                )
            )
        }
        raceDao.insertRaces(raceEntities)
        raceDao.insertSessions(sessionEntities)
    }

    override suspend fun updateSessionAlarm(sessionId: String, isSet: Boolean) {
        raceDao.updateAlarmStatus(sessionId, isSet)
    }

    private fun formatTimestamp(date: String, time: String?, defaultTime: String): String {
        val rawTime = time ?: defaultTime
        return if (rawTime.endsWith("Z")) "${date}T$rawTime" else "${date}T${rawTime}Z"
    }

    private fun mapSessionType(typeStr: String): SessionType = when (typeStr) {
        "QUALIFYING" -> SessionType.QUALIFYING
        "SPRINT" -> SessionType.SPRINT
        "FP1" -> SessionType.PRACTICE_1
        "FP2" -> SessionType.PRACTICE_2
        "FP3" -> SessionType.PRACTICE_3
        else -> SessionType.RACE
    }
}
