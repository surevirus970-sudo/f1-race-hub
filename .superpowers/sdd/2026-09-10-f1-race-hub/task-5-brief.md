# Task 5 Brief: Domain Models & Offline-First Repositories (TDD)

## Files to Create
- `app/src/main/java/com/f1racehub/app/domain/model/F1Models.kt`
- `app/src/main/java/com/f1racehub/app/domain/repository/RaceRepository.kt`
- `app/src/main/java/com/f1racehub/app/data/repository/RaceRepositoryImpl.kt`
- `app/src/test/java/com/f1racehub/app/data/repository/RaceRepositoryImplTest.kt`

## Interfaces
- Consumes: `RaceDao`, `F1ApiClient`, `DateTimeFormatterUtil`.
- Produces:
  - `GrandPrix`, `RaceSession`, `SessionType`
  - `RaceRepository`:
    - `fun observeGrandPrixList(): Flow<List<GrandPrix>>`
    - `suspend fun refreshSchedule(): Result<Unit>`
    - `suspend fun updateSessionAlarm(sessionId: String, isSet: Boolean)`

## Specific Requirements
1. **Preserve User Alarms Across Network Sync:** In `refreshSchedule()`, obtain current scheduled alarms using `raceDao.getScheduledAlarms()` (or check existing `isAlarmSet` flags) before inserting new sessions, so user alarms are not erased when fresh schedule data arrives from the API.
2. **Offline-First:** `observeGrandPrixList()` continuously reads from `raceDao.observeRacesWithSessions()`, converting local entities into domain models.
3. **Time Conversion:** Each session's `startTimeUtc` is converted to `ZonedDateTime` via `DateTimeFormatterUtil.parseUtcToLocalDateTime(s.startTimeUtc)`.

## Implementation Details

### 1. `F1Models.kt`
```kotlin
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
```

### 2. `RaceRepository.kt`
```kotlin
package com.f1racehub.app.domain.repository

import com.f1racehub.app.domain.model.GrandPrix
import kotlinx.coroutines.flow.Flow

interface RaceRepository {
    fun observeGrandPrixList(): Flow<List<GrandPrix>>
    suspend fun refreshSchedule(): Result<Unit>
    suspend fun updateSessionAlarm(sessionId: String, isSet: Boolean)
}
```

### 3. `RaceRepositoryImpl.kt`
```kotlin
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
                sessionEntities.add(RaceSessionEntity(id, round, "FP1", "${fp1.date}T${fp1.time ?: "10:30:00Z"}", existingAlarms.contains(id)))
            }
            r.qualifying?.let { q ->
                val id = "${round}_QUALIFYING"
                sessionEntities.add(RaceSessionEntity(id, round, "QUALIFYING", "${q.date}T${q.time ?: "15:00:00Z"}", existingAlarms.contains(id)))
            }
            r.sprint?.let { sp ->
                val id = "${round}_SPRINT"
                sessionEntities.add(RaceSessionEntity(id, round, "SPRINT", "${sp.date}T${sp.time ?: "12:00:00Z"}", existingAlarms.contains(id)))
            }
            val raceId = "${round}_RACE"
            sessionEntities.add(RaceSessionEntity(raceId, round, "RACE", "${r.date}T${r.time ?: "13:00:00Z"}", existingAlarms.contains(raceId)))
        }
        raceDao.insertRaces(raceEntities)
        raceDao.insertSessions(sessionEntities)
    }

    override suspend fun updateSessionAlarm(sessionId: String, isSet: Boolean) {
        raceDao.updateAlarmStatus(sessionId, isSet)
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
```

### 4. Unit Test `RaceRepositoryImplTest.kt`
Unit tests with MockK or Fake implementations verifying:
- Flow emission of domain models from DAO relations.
- Network sync saving to DAO.
- Preservation of alarm status when network refresh occurs.
- Failure handling when API throws exception (returns `Result.failure`).

### 5. Commit
Commit with message: `feat(repository): implement offline-first RaceRepositoryImpl and domain models`
