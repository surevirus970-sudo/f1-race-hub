package com.f1racehub.app.presentation.screens.timing

import app.cash.turbine.test
import com.f1racehub.app.data.remote.F1ApiClient
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveTimingViewModelTest {

    private lateinit var apiClient: F1ApiClient
    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        apiClient = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state when shouldPoll is false has empty rows and isLive false`() = runTest(testDispatcher) {
        val viewModel = LiveTimingViewModel(apiClient, shouldPoll = false)

        val state = viewModel.state.value
        assertTrue(state.rows.isEmpty(), "Initial rows should be empty when polling is disabled")
        assertFalse(state.isLive, "Initial state should have isLive == false")
        assertEquals("GREEN", state.flagStatus, "Initial flagStatus should be GREEN")
    }

    @Test
    fun `startPollingTiming populates exactly 10 drivers ordered from P1 to P10`() = runTest(testDispatcher) {
        val viewModel = LiveTimingViewModel(apiClient, shouldPoll = true)
        testScheduler.runCurrent()

        val state = viewModel.state.value
        assertTrue(state.isLive, "isLive should be true after polling runs")
        assertEquals(10, state.rows.size, "Timing tower must contain exactly 10 driver rows")

        val positions = state.rows.map { it.position }
        assertEquals((1..10).toList(), positions, "Driver rows must be strictly ordered from P1 to P10")

        viewModel.stopPollingTiming()
    }

    @Test
    fun `leader driver at position 1 has gap formatted strictly as LEADER`() = runTest(testDispatcher) {
        val viewModel = LiveTimingViewModel(apiClient, shouldPoll = true)
        testScheduler.runCurrent()

        val leader = viewModel.state.value.rows.first()
        assertEquals(1, leader.position, "First row must be position 1")
        assertEquals("VER", leader.driverCode, "Leader driver code should be VER")
        assertEquals(1, leader.driverNumber, "Leader driver number should be 1")
        assertEquals("LEADER", leader.gapToLeader, "Leader gap string must be LEADER")

        viewModel.stopPollingTiming()
    }

    @Test
    fun `non leader drivers from P2 to P10 have positive gap offsets`() = runTest(testDispatcher) {
        val viewModel = LiveTimingViewModel(apiClient, shouldPoll = true)
        testScheduler.runCurrent()

        val nonLeaders = viewModel.state.value.rows.drop(1)
        assertEquals(9, nonLeaders.size)
        assertTrue(
            nonLeaders.all { it.gapToLeader.startsWith("+") },
            "All drivers from P2 to P10 must have gaps prefixed with +"
        )

        viewModel.stopPollingTiming()
    }

    @Test
    fun `driver rows contain valid tyre compound and positive lap count`() = runTest(testDispatcher) {
        val viewModel = LiveTimingViewModel(apiClient, shouldPoll = true)
        testScheduler.runCurrent()

        val rows = viewModel.state.value.rows
        val validCompounds = setOf("SOFT", "MEDIUM", "HARD", "INTERMEDIATE", "WET")

        assertTrue(
            rows.all { it.tyreCompound in validCompounds },
            "All rows must have recognized Pirelli compound names"
        )
        assertTrue(
            rows.all { it.tyreLaps > 0 },
            "All rows must have tyre laps greater than 0"
        )

        viewModel.stopPollingTiming()
    }

    @Test
    fun `driver rows contain valid hex color codes`() = runTest(testDispatcher) {
        val viewModel = LiveTimingViewModel(apiClient, shouldPoll = true)
        testScheduler.runCurrent()

        val rows = viewModel.state.value.rows
        val hexRegex = Regex("^#[0-9A-Fa-f]{6}$")

        assertTrue(
            rows.all { it.teamColorHex.matches(hexRegex) },
            "All rows must have valid 6-character hex color codes with # prefix"
        )

        viewModel.stopPollingTiming()
    }

    @Test
    fun `updateFlag updates flagStatus across GREEN, YELLOW, and RED`() = runTest(testDispatcher) {
        val viewModel = LiveTimingViewModel(apiClient, shouldPoll = false)

        assertEquals("GREEN", viewModel.state.value.flagStatus)

        viewModel.updateFlag("YELLOW")
        assertEquals("YELLOW", viewModel.state.value.flagStatus, "Flag should update to YELLOW")

        viewModel.updateFlag("RED")
        assertEquals("RED", viewModel.state.value.flagStatus, "Flag should update to RED")

        viewModel.updateFlag("GREEN")
        assertEquals("GREEN", viewModel.state.value.flagStatus, "Flag should update back to GREEN")
    }

    @Test
    fun `manual startPollingTiming updates state when shouldPoll was false`() = runTest(testDispatcher) {
        val viewModel = LiveTimingViewModel(apiClient, shouldPoll = false)
        assertEquals(0, viewModel.state.value.rows.size)
        assertFalse(viewModel.state.value.isLive)

        viewModel.startPollingTiming()
        testScheduler.runCurrent()

        assertEquals(10, viewModel.state.value.rows.size)
        assertTrue(viewModel.state.value.isLive)

        viewModel.stopPollingTiming()
    }
}
