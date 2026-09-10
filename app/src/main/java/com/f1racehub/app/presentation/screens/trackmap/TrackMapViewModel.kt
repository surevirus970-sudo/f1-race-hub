package com.f1racehub.app.presentation.screens.trackmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1racehub.app.data.remote.F1ApiClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class CarTrackPosition(
    val driverNumber: Int,
    val driverCode: String,
    val teamColorHex: String,
    val normX: Float,
    val normY: Float
)

data class TrackMapUiState(
    val circuitName: String = "Autodromo Nazionale Monza",
    val cars: List<CarTrackPosition> = emptyList(),
    val isLoading: Boolean = false
)

class TrackMapViewModel(
    private val apiClient: F1ApiClient? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    shouldSimulate: Boolean = true
) : ViewModel() {

    private val _state = MutableStateFlow(
        TrackMapUiState(
            circuitName = "Autodromo Nazionale Monza",
            cars = calculateCarPositions(0.0f)
        )
    )
    val state: StateFlow<TrackMapUiState> = _state.asStateFlow()

    private var simulationJob: Job? = null
    private var currentStep = 0.0f

    init {
        if (shouldSimulate) {
            startSimulation()
        }
    }

    fun normalizeCoordinates(
        x: Double,
        y: Double,
        minX: Double,
        maxX: Double,
        minY: Double,
        maxY: Double
    ): Pair<Float, Float> {
        val normX = if (maxX > minX) {
            ((x - minX) / (maxX - minX)).toFloat().coerceIn(0.0f, 1.0f)
        } else {
            0.0f
        }
        val normY = if (maxY > minY) {
            ((y - minY) / (maxY - minY)).toFloat().coerceIn(0.0f, 1.0f)
        } else {
            0.0f
        }
        return Pair(normX, normY)
    }

    fun updateCars(newCars: List<CarTrackPosition>) {
        _state.update { it.copy(cars = newCars) }
    }

    fun advanceSimulationStep() {
        currentStep += 0.05f
        val simulatedCars = calculateCarPositions(currentStep)
        _state.update { it.copy(cars = simulatedCars) }
    }

    fun startSimulation(): Job {
        simulationJob?.cancel()
        val job = viewModelScope.launch(dispatcher) {
            while (true) {
                delay(100)
                advanceSimulationStep()
            }
        }
        simulationJob = job
        return job
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    private fun calculateCarPositions(step: Float): List<CarTrackPosition> {
        val drivers = listOf(
            DriverConfig(driverNumber = 1, driverCode = "VER", teamColorHex = "#3671C6", phaseOffset = 0.00f),
            DriverConfig(driverNumber = 4, driverCode = "NOR", teamColorHex = "#FF8000", phaseOffset = -0.35f),
            DriverConfig(driverNumber = 16, driverCode = "LEC", teamColorHex = "#E8002D", phaseOffset = -0.75f),
            DriverConfig(driverNumber = 44, driverCode = "HAM", teamColorHex = "#27F4D2", phaseOffset = -1.25f)
        )

        return drivers.map { driver ->
            val angle = (step + driver.phaseOffset).toDouble()
            val x = (0.50f + 0.35f * cos(angle)).toFloat().coerceIn(0.0f, 1.0f)
            val y = (0.50f + 0.35f * sin(angle)).toFloat().coerceIn(0.0f, 1.0f)
            CarTrackPosition(
                driverNumber = driver.driverNumber,
                driverCode = driver.driverCode,
                teamColorHex = driver.teamColorHex,
                normX = x,
                normY = y
            )
        }
    }

    private data class DriverConfig(
        val driverNumber: Int,
        val driverCode: String,
        val teamColorHex: String,
        val phaseOffset: Float
    )
}
