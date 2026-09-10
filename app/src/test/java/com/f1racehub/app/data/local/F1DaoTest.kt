package com.f1racehub.app.data.local

import com.f1racehub.app.data.local.dao.RaceDao
import com.f1racehub.app.data.local.entities.RaceEntity
import com.f1racehub.app.data.local.entities.RaceSessionEntity
import com.f1racehub.app.data.local.entities.RaceWithSessions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FakeRaceDao : RaceDao {
    private val races = mutableMapOf<Int, RaceEntity>()
    private val sessions = mutableMapOf<String, RaceSessionEntity>()
    private val updateTrigger = MutableStateFlow(0)

    override suspend fun insertRaces(races: List<RaceEntity>) {
        races.forEach { this.races[it.round] = it }
        updateTrigger.value += 1
    }

    override suspend fun insertSessions(sessions: List<RaceSessionEntity>) {
        sessions.forEach { this.sessions[it.sessionId] = it }
        updateTrigger.value += 1
    }

    override fun observeRacesWithSessions(): Flow<List<RaceWithSessions>> {
        return updateTrigger.map {
            races.values
                .sortedBy { it.round }
                .map { race ->
                    val matchingSessions = sessions.values
                        .filter { it.round == race.round }
                        .sortedBy { it.sessionId }
                    RaceWithSessions(race = race, sessions = matchingSessions)
                }
        }
    }

    override suspend fun updateAlarmStatus(sessionId: String, isSet: Boolean) {
        val existing = sessions[sessionId]
        if (existing != null) {
            sessions[sessionId] = existing.copy(isAlarmSet = isSet)
            updateTrigger.value += 1
        }
    }

    override suspend fun getScheduledAlarms(): List<RaceSessionEntity> {
        return sessions.values
            .filter { it.isAlarmSet }
            .sortedBy { it.sessionId }
    }
}

class F1DaoTest {

    private lateinit var dao: RaceDao

    @BeforeEach
    fun setUp() {
        dao = FakeRaceDao()
    }

    @Test
    fun `insertRaces and insertSessions should populate database and observeRacesWithSessions emits ordered list`() = runBlocking {
        val raceRound2 = RaceEntity(
            round = 2,
            raceName = "Chinese Grand Prix",
            circuitId = "shanghai",
            circuitName = "Shanghai International Circuit",
            country = "China",
            locality = "Shanghai",
            isCompleted = false
        )
        val raceRound1 = RaceEntity(
            round = 1,
            raceName = "Bahrain Grand Prix",
            circuitId = "bahrain",
            circuitName = "Bahrain International Circuit",
            country = "Bahrain",
            locality = "Sakhir",
            isCompleted = false
        )

        val session1Fp1 = RaceSessionEntity("1_FP1", 1, "FP1", "2026-02-27T11:30:00Z", false)
        val session1Race = RaceSessionEntity("1_RACE", 1, "RACE", "2026-03-01T15:00:00Z", false)
        val session2Race = RaceSessionEntity("2_RACE", 2, "RACE", "2026-03-22T07:00:00Z", false)

        dao.insertRaces(listOf(raceRound2, raceRound1))
        dao.insertSessions(listOf(session1Fp1, session1Race, session2Race))

        val observed = dao.observeRacesWithSessions().first()

        assertEquals(2, observed.size)
        assertEquals(1, observed[0].race.round)
        assertEquals("Bahrain Grand Prix", observed[0].race.raceName)
        assertEquals(2, observed[0].sessions.size)
        assertEquals("1_FP1", observed[0].sessions[0].sessionId)
        assertEquals("1_RACE", observed[0].sessions[1].sessionId)

        assertEquals(2, observed[1].race.round)
        assertEquals("Chinese Grand Prix", observed[1].race.raceName)
        assertEquals(1, observed[1].sessions.size)
        assertEquals("2_RACE", observed[1].sessions[0].sessionId)
    }

    @Test
    fun `insertRaces should replace existing race when round matches`() = runBlocking {
        val initialRace = RaceEntity(
            round = 1,
            raceName = "Bahrain Grand Prix",
            circuitId = "bahrain",
            circuitName = "Bahrain International Circuit",
            country = "Bahrain",
            locality = "Sakhir",
            isCompleted = false
        )
        dao.insertRaces(listOf(initialRace))

        val updatedRace = initialRace.copy(
            raceName = "Formula 1 Gulf Air Bahrain GP",
            isCompleted = true
        )
        dao.insertRaces(listOf(updatedRace))

        val observed = dao.observeRacesWithSessions().first()
        assertEquals(1, observed.size)
        assertEquals("Formula 1 Gulf Air Bahrain GP", observed[0].race.raceName)
        assertTrue(observed[0].race.isCompleted)
    }

    @Test
    fun `insertSessions should replace existing session when sessionId matches`() = runBlocking {
        val initialSession = RaceSessionEntity(
            sessionId = "1_QUALIFYING",
            round = 1,
            sessionType = "QUALIFYING",
            startTimeUtc = "2026-02-28T16:00:00Z",
            isAlarmSet = false
        )
        dao.insertSessions(listOf(initialSession))

        val updatedSession = initialSession.copy(
            startTimeUtc = "2026-02-28T17:00:00Z",
            isAlarmSet = true
        )
        dao.insertSessions(listOf(updatedSession))

        val alarms = dao.getScheduledAlarms()
        assertEquals(1, alarms.size)
        assertEquals("2026-02-28T17:00:00Z", alarms[0].startTimeUtc)
        assertTrue(alarms[0].isAlarmSet)
    }

    @Test
    fun `updateAlarmStatus should toggle alarm state for target session`() = runBlocking {
        val session1 = RaceSessionEntity("1_RACE", 1, "RACE", "2026-03-01T15:00:00Z", isAlarmSet = false)
        val session2 = RaceSessionEntity("2_RACE", 2, "RACE", "2026-03-22T07:00:00Z", isAlarmSet = false)
        dao.insertSessions(listOf(session1, session2))

        assertEquals(0, dao.getScheduledAlarms().size)

        dao.updateAlarmStatus("1_RACE", true)

        val activeAlarms = dao.getScheduledAlarms()
        assertEquals(1, activeAlarms.size)
        assertEquals("1_RACE", activeAlarms[0].sessionId)
        assertTrue(activeAlarms[0].isAlarmSet)

        dao.updateAlarmStatus("1_RACE", false)
        assertEquals(0, dao.getScheduledAlarms().size)
    }

    @Test
    fun `getScheduledAlarms should return only sessions where isAlarmSet is true`() = runBlocking {
        val session1 = RaceSessionEntity("1_FP1", 1, "FP1", "2026-02-27T11:30:00Z", isAlarmSet = true)
        val session2 = RaceSessionEntity("1_FP2", 1, "FP2", "2026-02-27T15:00:00Z", isAlarmSet = false)
        val session3 = RaceSessionEntity("1_RACE", 1, "RACE", "2026-03-01T15:00:00Z", isAlarmSet = true)
        dao.insertSessions(listOf(session1, session2, session3))

        val alarms = dao.getScheduledAlarms()
        assertEquals(2, alarms.size)
        assertEquals("1_FP1", alarms[0].sessionId)
        assertEquals("1_RACE", alarms[1].sessionId)
    }

    @Test
    fun `getScheduledAlarms should return empty list when no alarms are scheduled`() = runBlocking {
        val session = RaceSessionEntity("3_RACE", 3, "RACE", "2026-04-05T06:00:00Z", isAlarmSet = false)
        dao.insertSessions(listOf(session))

        val alarms = dao.getScheduledAlarms()
        assertTrue(alarms.isEmpty())
    }

    @Test
    fun `observeRacesWithSessions should reactively emit updated state when new sessions are added`() = runBlocking {
        val race = RaceEntity(
            round = 3,
            raceName = "Australian Grand Prix",
            circuitId = "albert_park",
            circuitName = "Albert Park Circuit",
            country = "Australia",
            locality = "Melbourne",
            isCompleted = false
        )
        dao.insertRaces(listOf(race))

        val initialList = dao.observeRacesWithSessions().first()
        assertEquals(1, initialList.size)
        assertEquals(0, initialList[0].sessions.size)

        val session = RaceSessionEntity("3_RACE", 3, "RACE", "2026-04-05T06:00:00Z", isAlarmSet = false)
        dao.insertSessions(listOf(session))

        val updatedList = dao.observeRacesWithSessions().first()
        assertEquals(1, updatedList.size)
        assertEquals(1, updatedList[0].sessions.size)
        assertEquals("3_RACE", updatedList[0].sessions[0].sessionId)
    }

    @Test
    fun `RaceWithSessions should correctly model 1-to-many relationship`() {
        val race = RaceEntity(
            round = 4,
            raceName = "Japanese Grand Prix",
            circuitId = "suzuka",
            circuitName = "Suzuka International Racing Course",
            country = "Japan",
            locality = "Suzuka",
            isCompleted = false
        )
        val sessions = listOf(
            RaceSessionEntity("4_FP1", 4, "FP1", "2026-04-10T02:30:00Z", false),
            RaceSessionEntity("4_FP2", 4, "FP2", "2026-04-10T06:00:00Z", false),
            RaceSessionEntity("4_QUALIFYING", 4, "QUALIFYING", "2026-04-11T06:00:00Z", false),
            RaceSessionEntity("4_RACE", 4, "RACE", "2026-04-12T05:00:00Z", false)
        )
        val raceWithSessions = RaceWithSessions(race = race, sessions = sessions)

        assertEquals(race, raceWithSessions.race)
        assertEquals(4, raceWithSessions.sessions.size)
        assertEquals("Suzuka", raceWithSessions.race.locality)
        assertEquals("4_QUALIFYING", raceWithSessions.sessions[2].sessionId)
    }

    @Test
    fun `RaceEntity and RaceSessionEntity default parameters should match schema requirements`() {
        val race = RaceEntity(
            round = 5,
            raceName = "Monaco Grand Prix",
            circuitId = "monaco",
            circuitName = "Circuit de Monaco",
            country = "Monaco",
            locality = "Monte-Carlo"
        )
        assertFalse(race.isCompleted)

        val session = RaceSessionEntity(
            sessionId = "5_RACE",
            round = 5,
            sessionType = "RACE",
            startTimeUtc = "2026-05-24T13:00:00Z"
        )
        assertFalse(session.isAlarmSet)
    }

    @Test
    fun `F1Database bytecode and interface should declare raceDao and Database annotation`() {
        val daoMethod = F1Database::class.java.methods.find { it.name == "raceDao" }
        assertEquals(RaceDao::class.java, daoMethod?.returnType)

        val stream = F1Database::class.java.classLoader.getResourceAsStream("com/f1racehub/app/data/local/F1Database.class")
        val bytecode = stream?.bufferedReader(Charsets.ISO_8859_1)?.readText() ?: ""
        assertTrue(bytecode.contains("Landroidx/room/Database;"))
        assertTrue(bytecode.contains("RaceEntity"))
        assertTrue(bytecode.contains("RaceSessionEntity"))
    }

    @Test
    fun `RaceDao bytecode should declare Room Dao and Query annotations`() {
        val methods = RaceDao::class.java.methods.map { it.name }
        assertTrue(methods.contains("insertRaces"))
        assertTrue(methods.contains("insertSessions"))
        assertTrue(methods.contains("observeRacesWithSessions"))
        assertTrue(methods.contains("updateAlarmStatus"))
        assertTrue(methods.contains("getScheduledAlarms"))

        val stream = RaceDao::class.java.classLoader.getResourceAsStream("com/f1racehub/app/data/local/dao/RaceDao.class")
        val bytecode = stream?.bufferedReader(Charsets.ISO_8859_1)?.readText() ?: ""
        assertTrue(bytecode.contains("Landroidx/room/Dao;"))
        assertTrue(bytecode.contains("Landroidx/room/Insert;"))
        assertTrue(bytecode.contains("Landroidx/room/Query;"))
        assertTrue(bytecode.contains("Landroidx/room/Transaction;"))
        assertTrue(bytecode.contains("SELECT * FROM races ORDER BY round ASC"))
        assertTrue(bytecode.contains("UPDATE race_sessions SET isAlarmSet = :isSet WHERE sessionId = :sessionId"))
        assertTrue(bytecode.contains("SELECT * FROM race_sessions WHERE isAlarmSet = 1"))
    }

    @Test
    fun `RaceEntities bytecode should declare Room Entity, ForeignKey, and Relation annotations`() {
        val raceStream = RaceEntity::class.java.classLoader.getResourceAsStream("com/f1racehub/app/data/local/entities/RaceEntity.class")
        val raceBytecode = raceStream?.bufferedReader(Charsets.ISO_8859_1)?.readText() ?: ""
        assertTrue(raceBytecode.contains("Landroidx/room/Entity;"))
        assertTrue(raceBytecode.contains("races"))
        assertTrue(raceBytecode.contains("Landroidx/room/PrimaryKey;"))

        val sessionStream = RaceSessionEntity::class.java.classLoader.getResourceAsStream("com/f1racehub/app/data/local/entities/RaceSessionEntity.class")
        val sessionBytecode = sessionStream?.bufferedReader(Charsets.ISO_8859_1)?.readText() ?: ""
        assertTrue(sessionBytecode.contains("Landroidx/room/Entity;"))
        assertTrue(sessionBytecode.contains("race_sessions"))
        assertTrue(sessionBytecode.contains("Landroidx/room/PrimaryKey;"))
        assertTrue(sessionBytecode.contains("Landroidx/room/ForeignKey;"))
        assertTrue(sessionBytecode.contains("Landroidx/room/Index;"))

        val withSessionsStream = RaceWithSessions::class.java.classLoader.getResourceAsStream("com/f1racehub/app/data/local/entities/RaceWithSessions.class")
        val withSessionsBytecode = withSessionsStream?.bufferedReader(Charsets.ISO_8859_1)?.readText() ?: ""
        assertTrue(withSessionsBytecode.contains("Landroidx/room/Embedded;"))
        assertTrue(withSessionsBytecode.contains("Landroidx/room/Relation;"))
    }
}
