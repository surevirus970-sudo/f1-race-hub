package com.f1racehub.app.presentation.screens.dashboard

import app.cash.turbine.test
import com.f1racehub.app.core.alarms.SessionAlarmScheduler
import com.f1racehub.app.domain.model.GrandPrix
import com.f1racehub.app.domain.model.RaceSession
import com.f1racehub.app.domain.model.SessionType
import com.f1racehub.app.domain.repository.RaceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var raceRepository: RaceRepository
    private lateinit var alarmScheduler: SessionAlarmScheduler
    private lateinit var testDispatcher: TestDispatcher

    private val fixedZone = ZoneId.of("UTC")
    private val sessionStartTime = ZonedDateTime.of(2026, 3, 29, 15, 0, 0, 0, fixedZone)

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        raceRepository = mockk(relaxed = true)
        alarmScheduler = mockk(relaxed = true)

        coEvery { raceRepository.refreshSchedule() } returns Result.success(Unit)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createSampleGrandPrix(
        round: Int,
        name: String,
        isCompleted: Boolean,
        sessions: List<RaceSession> = emptyList()
    ): GrandPrix {
        return GrandPrix(
            round = round,
            name = name,
            circuitName = "Albert Park",
            country = "Australia",
            locality = "Melbourne",
            sessions = sessions,
            isCompleted = isCompleted
        )
    }

    private fun createSampleSession(
        id: String = "session_1",
        type: SessionType = SessionType.QUALIFYING,
        startTime: ZonedDateTime = sessionStartTime,
        isAlarmSet: Boolean = false
    ): RaceSession {
        return RaceSession(
            id = id,
            type = type,
            startTime = startTime,
            isAlarmSet = isAlarmSet
        )
    }

    @Test
    fun `initial uiState starts with loading true and null nextGrandPrix`() = runTest(testDispatcher) {
        val racesFlow = MutableSharedFlow<List<GrandPrix>>()
        every { raceRepository.observeGrandPrixList() } returns racesFlow

        val viewModel = DashboardViewModel(raceRepository, alarmScheduler)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading, "Initial state should have isLoading == true")
            assertNull(initialState.nextGrandPrix, "Initial state should have nextGrandPrix == null")
            assertNull(initialState.errorMessage, "Initial state should have errorMessage == null")
        }
    }

    @Test
    fun `loadDashboardData updates state with first non-completed race and marks loading false`() = runTest(testDispatcher) {
        val completedRace = createSampleGrandPrix(round = 1, name = "Bahrain GP", isCompleted = true)
        val upcomingRace = createSampleGrandPrix(round = 2, name = "Saudi Arabian GP", isCompleted = false)
        val futureRace = createSampleGrandPrix(round = 3, name = "Australian GP", isCompleted = false)

        every { raceRepository.observeGrandPrixList() } returns flowOf(listOf(completedRace, upcomingRace, futureRace))

        val viewModel = DashboardViewModel(raceRepository, alarmScheduler)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.isLoading, "Initial state must have isLoading == true")
            advanceUntilIdle()
            val state = awaitItem()
            assertFalse(state.isLoading, "Loaded state must set isLoading to false")
            assertNotNull(state.nextGrandPrix, "Upcoming race must be present")
            assertEquals("Saudi Arabian GP", state.nextGrandPrix?.name)
            assertEquals(2, state.nextGrandPrix?.round)
        }
    }

    @Test
    fun `loadDashboardData sets nextGrandPrix to null when all races are completed`() = runTest(testDispatcher) {
        val completed1 = createSampleGrandPrix(round = 1, name = "Bahrain GP", isCompleted = true)
        val completed2 = createSampleGrandPrix(round = 2, name = "Saudi GP", isCompleted = true)

        every { raceRepository.observeGrandPrixList() } returns flowOf(listOf(completed1, completed2))

        val viewModel = DashboardViewModel(raceRepository, alarmScheduler)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.isLoading)
            advanceUntilIdle()
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.nextGrandPrix, "All completed races should resolve to null nextGrandPrix")
        }
    }

    @Test
    fun `viewModel initialization triggers raceRepository refreshSchedule`() = runTest(testDispatcher) {
        every { raceRepository.observeGrandPrixList() } returns flowOf(emptyList())

        DashboardViewModel(raceRepository, alarmScheduler)
        advanceUntilIdle()

        coVerify(exactly = 1) { raceRepository.refreshSchedule() }
    }

    @Test
    fun `toggleAlarm when alarm is not set schedules alarm 15 minutes prior and updates repository`() = runTest(testDispatcher) {
        every { raceRepository.observeGrandPrixList() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(raceRepository, alarmScheduler)
        advanceUntilIdle()

        val session = createSampleSession(
            id = "aus_gp_q",
            type = SessionType.QUALIFYING,
            startTime = sessionStartTime,
            isAlarmSet = false
        )
        val raceName = "Australian Grand Prix"
        val expectedTriggerMillis = sessionStartTime.toInstant().toEpochMilli() - (15 * 60 * 1000)

        viewModel.toggleAlarm(session, raceName)
        advanceUntilIdle()

        verify(exactly = 1) {
            alarmScheduler.scheduleAlarm(
                sessionId = "aus_gp_q",
                raceName = raceName,
                sessionType = SessionType.QUALIFYING.name,
                triggerTimeMillis = expectedTriggerMillis
            )
        }
        coVerify(exactly = 1) {
            raceRepository.updateSessionAlarm("aus_gp_q", true)
        }
        verify(exactly = 0) {
            alarmScheduler.cancelAlarm(any())
        }
    }

    @Test
    fun `toggleAlarm when alarm is set cancels alarm and updates repository to false`() = runTest(testDispatcher) {
        every { raceRepository.observeGrandPrixList() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(raceRepository, alarmScheduler)
        advanceUntilIdle()

        val session = createSampleSession(
            id = "aus_gp_race",
            type = SessionType.RACE,
            startTime = sessionStartTime,
            isAlarmSet = true
        )
        val raceName = "Australian Grand Prix"

        viewModel.toggleAlarm(session, raceName)
        advanceUntilIdle()

        verify(exactly = 1) {
            alarmScheduler.cancelAlarm(sessionId = "aus_gp_race")
        }
        coVerify(exactly = 1) {
            raceRepository.updateSessionAlarm("aus_gp_race", false)
        }
        verify(exactly = 0) {
            alarmScheduler.scheduleAlarm(any(), any(), any(), any())
        }
    }

    @Test
    fun `toggleAlarm computes exact millisecond subtraction for multiple session types`() = runTest(testDispatcher) {
        every { raceRepository.observeGrandPrixList() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(raceRepository, alarmScheduler)
        advanceUntilIdle()

        val sprintSession = createSampleSession(
            id = "monaco_sprint",
            type = SessionType.SPRINT,
            startTime = sessionStartTime.plusDays(1),
            isAlarmSet = false
        )
        val expectedMillis = sprintSession.startTime.toInstant().toEpochMilli() - 900_000L

        viewModel.toggleAlarm(sprintSession, "Monaco Grand Prix")
        advanceUntilIdle()

        verify(exactly = 1) {
            alarmScheduler.scheduleAlarm("monaco_sprint", "Monaco Grand Prix", "SPRINT", expectedMillis)
        }
        coVerify(exactly = 1) {
            raceRepository.updateSessionAlarm("monaco_sprint", true)
        }
    }
}
