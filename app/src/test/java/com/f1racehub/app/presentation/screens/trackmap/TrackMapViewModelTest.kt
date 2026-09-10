package com.f1racehub.app.presentation.screens.trackmap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class TrackMapViewModelTest {

    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state emits correct circuit name and valid initial car list`() = runTest(testDispatcher) {
        val viewModel = TrackMapViewModel(dispatcher = testDispatcher, shouldSimulate = false)

        val state = viewModel.state.value
        assertEquals("Autodromo Nazionale Monza", state.circuitName, "Circuit name must match Autodromo Nazionale Monza")
        assertFalse(state.isLoading, "Initial state should not be loading")
        assertEquals(4, state.cars.size, "Initial car count must be 4")

        val hexRegex = Regex("^#[0-9A-Fa-f]{6}$")
        for (car in state.cars) {
            assertTrue(car.driverNumber > 0, "Driver number must be positive")
            assertTrue(car.driverCode.isNotBlank(), "Driver code must not be blank")
            assertTrue(car.teamColorHex.matches(hexRegex), "Team color hex must match format #RRGGBB")
            assertTrue(car.normX in 0.0f..1.0f, "normX must be in 0.0f..1.0f")
            assertTrue(car.normY in 0.0f..1.0f, "normY must be in 0.0f..1.0f")
        }
    }

    @Test
    fun `normalizeCoordinates accurately maps raw telemetry to unit interval and clamps out-of-bound values`() = runTest(testDispatcher) {
        val viewModel = TrackMapViewModel(dispatcher = testDispatcher, shouldSimulate = false)

        // Linear interpolation check
        val (normX, normY) = viewModel.normalizeCoordinates(
            x = 50.0,
            y = 75.0,
            minX = 0.0,
            maxX = 100.0,
            minY = 0.0,
            maxY = 100.0
        )
        assertEquals(0.50f, normX, 0.001f, "normX should map exactly to 0.5")
        assertEquals(0.75f, normY, 0.001f, "normY should map exactly to 0.75")

        // Clamping negative out-of-bounds values
        val (clampedMinX, clampedMinY) = viewModel.normalizeCoordinates(
            x = -150.0,
            y = -30.0,
            minX = 0.0,
            maxX = 100.0,
            minY = 0.0,
            maxY = 100.0
        )
        assertEquals(0.0f, clampedMinX, "normX below min must clamp to 0.0f")
        assertEquals(0.0f, clampedMinY, "normY below min must clamp to 0.0f")

        // Clamping positive out-of-bounds values
        val (clampedMaxX, clampedMaxY) = viewModel.normalizeCoordinates(
            x = 350.0,
            y = 120.0,
            minX = 0.0,
            maxX = 100.0,
            minY = 0.0,
            maxY = 100.0
        )
        assertEquals(1.0f, clampedMaxX, "normX above max must clamp to 1.0f")
        assertEquals(1.0f, clampedMaxY, "normY above max must clamp to 1.0f")

        // Degenerate bounds guard
        val (safeDegenerateX, safeDegenerateY) = viewModel.normalizeCoordinates(
            x = 50.0,
            y = 50.0,
            minX = 10.0,
            maxX = 10.0,
            minY = 10.0,
            maxY = 10.0
        )
        assertEquals(0.0f, safeDegenerateX, "Degenerate X span must default safely to 0.0f")
        assertEquals(0.0f, safeDegenerateY, "Degenerate Y span must default safely to 0.0f")
    }

    @Test
    fun `updateCars updates the state flow immediately`() = runTest(testDispatcher) {
        val viewModel = TrackMapViewModel(dispatcher = testDispatcher, shouldSimulate = false)

        val updatedTelemetry = listOf(
            CarTrackPosition(driverNumber = 81, driverCode = "PIA", teamColorHex = "#FF8000", normX = 0.25f, normY = 0.35f),
            CarTrackPosition(driverNumber = 55, driverCode = "SAI", teamColorHex = "#E8002D", normX = 0.65f, normY = 0.85f)
        )

        viewModel.updateCars(updatedTelemetry)

        val currentState = viewModel.state.value
        assertEquals(updatedTelemetry, currentState.cars, "state.cars must match updatedTelemetry exactly")
        assertEquals(2, currentState.cars.size, "Car list size should be 2")
    }

    @Test
    fun `simulation step advances car coordinates within the valid normalized range from 0f to 1f`() = runTest(testDispatcher) {
        val viewModel = TrackMapViewModel(dispatcher = testDispatcher, shouldSimulate = false)
        val initialCars = viewModel.state.value.cars

        viewModel.advanceSimulationStep()
        val advancedCars = viewModel.state.value.cars

        assertEquals(initialCars.size, advancedCars.size, "Advanced cars list size must remain constant")

        val hasMoved = advancedCars.zip(initialCars).any { (next, prev) ->
            next.normX != prev.normX || next.normY != prev.normY
        }
        assertTrue(hasMoved, "At least one car position must change after simulation step")

        assertTrue(
            advancedCars.all { it.normX in 0.0f..1.0f && it.normY in 0.0f..1.0f },
            "All car coordinates after step advancement must remain within [0.0f, 1.0f]"
        )
    }

    @Test
    fun `simulation ticker advances coordinates periodically and can be stopped`() = runTest(testDispatcher) {
        val viewModel = TrackMapViewModel(dispatcher = testDispatcher, shouldSimulate = false)
        val initialCars = viewModel.state.value.cars

        viewModel.startSimulation()
        testScheduler.advanceTimeBy(250)

        val activeCars = viewModel.state.value.cars
        assertNotEquals(initialCars, activeCars, "Car positions should change after 250ms elapsed time")

        viewModel.stopSimulation()
        val pausedCars = viewModel.state.value.cars

        testScheduler.advanceTimeBy(300)
        assertEquals(pausedCars, viewModel.state.value.cars, "Car positions must freeze after stopSimulation")
    }

    @Test
    fun `koin resolves TrackMapViewModel from appModule`() {
        val mockContext = io.mockk.mockk<android.content.Context>(relaxed = true)
        val mockDatabase = io.mockk.mockk<com.f1racehub.app.data.local.F1Database>()
        val mockDao = io.mockk.mockk<com.f1racehub.app.data.local.dao.RaceDao>()
        io.mockk.every { mockDatabase.raceDao() } returns mockDao

        val koinApp = koinApplication {
            androidContext(mockContext)
            allowOverride(true)
            modules(
                com.f1racehub.app.di.appModule,
                module {
                    single<com.f1racehub.app.data.local.F1Database> { mockDatabase }
                }
            )
        }

        try {
            val resolvedVm = koinApp.koin.get<TrackMapViewModel>()
            assertNotNull(resolvedVm, "TrackMapViewModel must resolve cleanly from appModule")
        } finally {
            koinApp.close()
        }
    }
}
