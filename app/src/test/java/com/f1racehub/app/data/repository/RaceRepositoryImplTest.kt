package com.f1racehub.app.data.repository

import com.f1racehub.app.data.local.dao.RaceDao
import com.f1racehub.app.data.local.entities.RaceEntity
import com.f1racehub.app.data.local.entities.RaceSessionEntity
import com.f1racehub.app.data.local.entities.RaceWithSessions
import com.f1racehub.app.data.remote.F1ApiClient
import com.f1racehub.app.domain.model.SessionType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.ZoneOffset

class FakeRaceDao : RaceDao {
    private val races = mutableMapOf<Int, RaceEntity>()
    private val sessions = mutableMapOf<String, RaceSessionEntity>()
    val flow = MutableStateFlow<List<RaceWithSessions>>(emptyList())

    override suspend fun insertRaces(races: List<RaceEntity>) {
        races.forEach { this.races[it.round] = it }
        emitCurrent()
    }

    override suspend fun insertSessions(sessions: List<RaceSessionEntity>) {
        sessions.forEach { this.sessions[it.sessionId] = it }
        emitCurrent()
    }

    override fun observeRacesWithSessions(): Flow<List<RaceWithSessions>> = flow

    override suspend fun updateAlarmStatus(sessionId: String, isSet: Boolean) {
        val session = sessions[sessionId]
        if (session != null) {
            sessions[sessionId] = session.copy(isAlarmSet = isSet)
            emitCurrent()
        }
    }

    override suspend fun getScheduledAlarms(): List<RaceSessionEntity> {
        return sessions.values.filter { it.isAlarmSet }
    }

    fun emitCurrent() {
        val result = races.values.sortedBy { it.round }.map { race ->
            val raceSessions = sessions.values.filter { it.round == race.round }.sortedBy { it.sessionId }
            RaceWithSessions(race = race, sessions = raceSessions)
        }
        flow.value = result
    }

    fun getAllRaces(): List<RaceEntity> = races.values.toList()
    fun getAllSessions(): List<RaceSessionEntity> = sessions.values.toList()
}

class RaceRepositoryImplTest {

    private lateinit var fakeRaceDao: FakeRaceDao

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @BeforeEach
    fun setUp() {
        fakeRaceDao = FakeRaceDao()
    }

    private fun createApiClient(engine: MockEngine): F1ApiClient {
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(jsonConfig) }
        }
        return F1ApiClient(httpClient)
    }

    @Test
    fun `observeGrandPrixList emits pure domain models mapped from Room entities`() = runBlocking {
        val race = RaceEntity(
            round = 1,
            raceName = "Bahrain Grand Prix",
            circuitId = "bahrain",
            circuitName = "Bahrain International Circuit",
            country = "Bahrain",
            locality = "Sakhir",
            isCompleted = false
        )
        val sessionFp1 = RaceSessionEntity(
            sessionId = "1_FP1",
            round = 1,
            sessionType = "FP1",
            startTimeUtc = "2026-02-27T11:30:00Z",
            isAlarmSet = false
        )
        val sessionFp2 = RaceSessionEntity(
            sessionId = "1_FP2",
            round = 1,
            sessionType = "FP2",
            startTimeUtc = "2026-02-27T15:00:00Z",
            isAlarmSet = false
        )
        val sessionFp3 = RaceSessionEntity(
            sessionId = "1_FP3",
            round = 1,
            sessionType = "FP3",
            startTimeUtc = "2026-02-28T12:30:00Z",
            isAlarmSet = false
        )
        val sessionQ = RaceSessionEntity(
            sessionId = "1_QUALIFYING",
            round = 1,
            sessionType = "QUALIFYING",
            startTimeUtc = "2026-02-28T16:00:00Z",
            isAlarmSet = true
        )
        val sessionRace = RaceSessionEntity(
            sessionId = "1_RACE",
            round = 1,
            sessionType = "RACE",
            startTimeUtc = "2026-03-01T15:00:00Z",
            isAlarmSet = false
        )

        fakeRaceDao.insertRaces(listOf(race))
        fakeRaceDao.insertSessions(listOf(sessionFp1, sessionFp2, sessionFp3, sessionQ, sessionRace))

        val mockEngine = MockEngine { respond("") }
        val repository = RaceRepositoryImpl(fakeRaceDao, createApiClient(mockEngine))

        val result = repository.observeGrandPrixList().first()
        assertEquals(1, result.size)

        val gp = result[0]
        assertEquals(1, gp.round)
        assertEquals("Bahrain Grand Prix", gp.name)
        assertEquals("Bahrain International Circuit", gp.circuitName)
        assertEquals("Bahrain", gp.country)
        assertEquals("Sakhir", gp.locality)
        assertFalse(gp.isCompleted)
        assertEquals(5, gp.sessions.size)

        val fp1 = gp.sessions.first { it.id == "1_FP1" }
        assertEquals(SessionType.PRACTICE_1, fp1.type)
        assertEquals(2026, fp1.startTime.year)
        assertEquals(2, fp1.startTime.monthValue)
        assertEquals(27, fp1.startTime.dayOfMonth)
        assertFalse(fp1.isAlarmSet)

        val fp2 = gp.sessions.first { it.id == "1_FP2" }
        assertEquals(SessionType.PRACTICE_2, fp2.type)

        val fp3 = gp.sessions.first { it.id == "1_FP3" }
        assertEquals(SessionType.PRACTICE_3, fp3.type)

        val qual = gp.sessions.first { it.id == "1_QUALIFYING" }
        assertEquals(SessionType.QUALIFYING, qual.type)
        assertTrue(qual.isAlarmSet)

        val raceSession = gp.sessions.first { it.id == "1_RACE" }
        assertEquals(SessionType.RACE, raceSession.type)
        assertFalse(raceSession.isAlarmSet)
    }

    @Test
    fun `observeGrandPrixList correctly maps Sprint session and unknown fallback type`() = runBlocking {
        val race = RaceEntity(
            round = 2,
            raceName = "Chinese Grand Prix",
            circuitId = "shanghai",
            circuitName = "Shanghai International Circuit",
            country = "China",
            locality = "Shanghai",
            isCompleted = true
        )
        val sessionSprint = RaceSessionEntity(
            sessionId = "2_SPRINT",
            round = 2,
            sessionType = "SPRINT",
            startTimeUtc = "2026-03-21T03:00:00Z",
            isAlarmSet = false
        )
        val sessionUnknown = RaceSessionEntity(
            sessionId = "2_OTHER",
            round = 2,
            sessionType = "UNKNOWN_SESSION",
            startTimeUtc = "2026-03-21T05:00:00Z",
            isAlarmSet = false
        )

        fakeRaceDao.insertRaces(listOf(race))
        fakeRaceDao.insertSessions(listOf(sessionSprint, sessionUnknown))

        val mockEngine = MockEngine { respond("") }
        val repository = RaceRepositoryImpl(fakeRaceDao, createApiClient(mockEngine))

        val result = repository.observeGrandPrixList().first()
        val gp = result[0]
        assertTrue(gp.isCompleted)

        val sprint = gp.sessions.first { it.id == "2_SPRINT" }
        assertEquals(SessionType.SPRINT, sprint.type)

        val other = gp.sessions.first { it.id == "2_OTHER" }
        assertEquals(SessionType.RACE, other.type)
    }

    @Test
    fun `refreshSchedule fetches remote races and persists entities into Room DAO`() = runBlocking {
        val sampleJson = """
        {
            "MRData": {
                "RaceTable": {
                    "season": "2026",
                    "Races": [
                        {
                            "season": "2026",
                            "round": "1",
                            "raceName": "Bahrain Grand Prix",
                            "Circuit": {
                                "circuitId": "bahrain",
                                "circuitName": "Bahrain International Circuit",
                                "Location": { "locality": "Sakhir", "country": "Bahrain" }
                            },
                            "date": "2026-03-01",
                            "time": "15:00:00Z",
                            "FirstPractice": { "date": "2026-02-27", "time": "11:30:00Z" },
                            "SecondPractice": { "date": "2026-02-27", "time": "15:00:00Z" },
                            "ThirdPractice": { "date": "2026-02-28", "time": "12:30:00Z" },
                            "Qualifying": { "date": "2026-02-28", "time": "16:00:00Z" }
                        },
                        {
                            "season": "2026",
                            "round": "2",
                            "raceName": "Chinese Grand Prix",
                            "Circuit": {
                                "circuitId": "shanghai",
                                "circuitName": "Shanghai International Circuit",
                                "Location": { "locality": "Shanghai", "country": "China" }
                            },
                            "date": "2026-03-22",
                            "time": "07:00:00Z",
                            "FirstPractice": { "date": "2026-03-20", "time": "03:30:00Z" },
                            "Qualifying": { "date": "2026-03-21", "time": "07:00:00Z" },
                            "Sprint": { "date": "2026-03-21", "time": "03:00:00Z" }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = sampleJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repository = RaceRepositoryImpl(fakeRaceDao, createApiClient(mockEngine))
        val refreshResult = repository.refreshSchedule()

        assertTrue(refreshResult.isSuccess)

        val races = fakeRaceDao.getAllRaces()
        assertEquals(2, races.size)
        assertEquals(1, races[0].round)
        assertEquals("Bahrain Grand Prix", races[0].raceName)
        assertEquals(2, races[1].round)
        assertEquals("Chinese Grand Prix", races[1].raceName)

        val sessions = fakeRaceDao.getAllSessions()
        assertEquals(9, sessions.size) // 5 for Bahrain, 4 for China

        val bahrainFp1 = sessions.first { it.sessionId == "1_FP1" }
        assertEquals(1, bahrainFp1.round)
        assertEquals("FP1", bahrainFp1.sessionType)
        assertEquals("2026-02-27T11:30:00Z", bahrainFp1.startTimeUtc)
        assertFalse(bahrainFp1.isAlarmSet)

        val chinaSprint = sessions.first { it.sessionId == "2_SPRINT" }
        assertEquals(2, chinaSprint.round)
        assertEquals("SPRINT", chinaSprint.sessionType)
        assertEquals("2026-03-21T03:00:00Z", chinaSprint.startTimeUtc)
        assertFalse(chinaSprint.isAlarmSet)
    }

    @Test
    fun `refreshSchedule should preserve user alarm settings across network refresh`() = runBlocking {
        // Pre-populate scheduled alarms in the local database
        val preExistingRace = RaceEntity(
            round = 1,
            raceName = "Old Bahrain GP",
            circuitId = "bahrain",
            circuitName = "Bahrain Circuit",
            country = "Bahrain",
            locality = "Sakhir"
        )
        val alarmFp1 = RaceSessionEntity(
            sessionId = "1_FP1",
            round = 1,
            sessionType = "FP1",
            startTimeUtc = "2026-02-27T11:30:00Z",
            isAlarmSet = true
        )
        val alarmRace = RaceSessionEntity(
            sessionId = "1_RACE",
            round = 1,
            sessionType = "RACE",
            startTimeUtc = "2026-03-01T15:00:00Z",
            isAlarmSet = true
        )
        val noAlarmQual = RaceSessionEntity(
            sessionId = "1_QUALIFYING",
            round = 1,
            sessionType = "QUALIFYING",
            startTimeUtc = "2026-02-28T16:00:00Z",
            isAlarmSet = false
        )

        fakeRaceDao.insertRaces(listOf(preExistingRace))
        fakeRaceDao.insertSessions(listOf(alarmFp1, alarmRace, noAlarmQual))

        val preAlarms = fakeRaceDao.getScheduledAlarms()
        assertEquals(2, preAlarms.size)

        val networkJson = """
        {
            "MRData": {
                "RaceTable": {
                    "season": "2026",
                    "Races": [
                        {
                            "season": "2026",
                            "round": "1",
                            "raceName": "Bahrain Grand Prix (Updated Schedule)",
                            "Circuit": {
                                "circuitId": "bahrain",
                                "circuitName": "Bahrain International Circuit",
                                "Location": { "locality": "Sakhir", "country": "Bahrain" }
                            },
                            "date": "2026-03-01",
                            "time": "15:00:00Z",
                            "FirstPractice": { "date": "2026-02-27", "time": "11:30:00Z" },
                            "Qualifying": { "date": "2026-02-28", "time": "16:00:00Z" }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = networkJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repository = RaceRepositoryImpl(fakeRaceDao, createApiClient(mockEngine))
        val refreshResult = repository.refreshSchedule()

        assertTrue(refreshResult.isSuccess)

        val updatedSessions = fakeRaceDao.getAllSessions()
        val fp1 = updatedSessions.first { it.sessionId == "1_FP1" }
        val raceSession = updatedSessions.first { it.sessionId == "1_RACE" }
        val qualSession = updatedSessions.first { it.sessionId == "1_QUALIFYING" }

        assertTrue(fp1.isAlarmSet, "1_FP1 alarm flag should be preserved as true")
        assertTrue(raceSession.isAlarmSet, "1_RACE alarm flag should be preserved as true")
        assertFalse(qualSession.isAlarmSet, "1_QUALIFYING alarm flag should remain false")

        val alarmsAfterRefresh = fakeRaceDao.getScheduledAlarms()
        assertEquals(2, alarmsAfterRefresh.size)
        assertTrue(alarmsAfterRefresh.any { it.sessionId == "1_FP1" })
        assertTrue(alarmsAfterRefresh.any { it.sessionId == "1_RACE" })
    }

    @Test
    fun `refreshSchedule handles nullable and missing session times by providing defaults`() = runBlocking {
        val sampleJson = """
        {
            "MRData": {
                "RaceTable": {
                    "season": "2026",
                    "Races": [
                        {
                            "season": "2026",
                            "round": "24",
                            "raceName": "Abu Dhabi Grand Prix",
                            "Circuit": {
                                "circuitId": "yas_marina",
                                "circuitName": "Yas Marina Circuit",
                                "Location": { "locality": "Abu Dhabi", "country": "UAE" }
                            },
                            "date": "2026-12-06",
                            "FirstPractice": { "date": "2026-12-04" }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = sampleJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repository = RaceRepositoryImpl(fakeRaceDao, createApiClient(mockEngine))
        val result = repository.refreshSchedule()

        assertTrue(result.isSuccess)
        val sessions = fakeRaceDao.getAllSessions()
        assertEquals(2, sessions.size) // FP1 and Race

        val fp1 = sessions.first { it.sessionId == "24_FP1" }
        assertEquals("2026-12-04T10:30:00Z", fp1.startTimeUtc)

        val race = sessions.first { it.sessionId == "24_RACE" }
        assertEquals("2026-12-06T13:00:00Z", race.startTimeUtc)
    }

    @Test
    fun `refreshSchedule returns Result failure when API throws network exception`() = runBlocking {
        val mockEngine = MockEngine {
            throw IOException("Failed to connect to Jolpica API server")
        }

        val repository = RaceRepositoryImpl(fakeRaceDao, createApiClient(mockEngine))
        val result = repository.refreshSchedule()

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals("Failed to connect to Jolpica API server", result.exceptionOrNull()?.message)
    }

    @Test
    fun `refreshSchedule returns Result failure when API returns HTTP 500 error`() = runBlocking {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.InternalServerError, "Server Error")
        }

        val repository = RaceRepositoryImpl(fakeRaceDao, createApiClient(mockEngine))
        val result = repository.refreshSchedule()

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `updateSessionAlarm updates alarm status in Room DAO`() = runBlocking {
        val session = RaceSessionEntity(
            sessionId = "1_RACE",
            round = 1,
            sessionType = "RACE",
            startTimeUtc = "2026-03-01T15:00:00Z",
            isAlarmSet = false
        )
        fakeRaceDao.insertSessions(listOf(session))

        val mockEngine = MockEngine { respond("") }
        val repository = RaceRepositoryImpl(fakeRaceDao, createApiClient(mockEngine))

        assertFalse(fakeRaceDao.getAllSessions()[0].isAlarmSet)

        repository.updateSessionAlarm("1_RACE", true)
        assertTrue(fakeRaceDao.getAllSessions()[0].isAlarmSet)

        repository.updateSessionAlarm("1_RACE", false)
        assertFalse(fakeRaceDao.getAllSessions()[0].isAlarmSet)
    }
}
