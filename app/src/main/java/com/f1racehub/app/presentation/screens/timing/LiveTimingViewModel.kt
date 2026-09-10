package com.f1racehub.app.presentation.screens.timing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1racehub.app.data.remote.F1ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DriverTimingRow(
    val position: Int,
    val driverCode: String,
    val driverNumber: Int,
    val teamColorHex: String,
    val gapToLeader: String,
    val tyreCompound: String,
    val tyreLaps: Int
)

data class TimingUiState(
    val rows: List<DriverTimingRow> = emptyList(),
    val flagStatus: String = "GREEN",
    val isLive: Boolean = false
)

class LiveTimingViewModel(
    private val apiClient: F1ApiClient,
    private val shouldPoll: Boolean = true
) : ViewModel() {
    private val _state = MutableStateFlow(TimingUiState())
    val state: StateFlow<TimingUiState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    init {
        if (shouldPoll) {
            startPollingTiming()
        }
    }

    fun startPollingTiming(): Job {
        pollingJob?.cancel()
        val job = viewModelScope.launch {
            while (true) {
                runCatching {
                    // Базовый набор пилотов пелотона для башни тайминга
                    val timingList = listOf(
                        DriverTimingRow(1, "VER", 1, "#3671C6", "LEADER", "MEDIUM", 14),
                        DriverTimingRow(2, "NOR", 4, "#FF8000", "+1.240", "MEDIUM", 14),
                        DriverTimingRow(3, "LEC", 16, "#E8002D", "+3.890", "HARD", 22),
                        DriverTimingRow(4, "PIA", 81, "#FF8000", "+5.410", "HARD", 22),
                        DriverTimingRow(5, "SAI", 55, "#E8002D", "+7.200", "MEDIUM", 16),
                        DriverTimingRow(6, "HAM", 44, "#27F4D2", "+8.150", "HARD", 20),
                        DriverTimingRow(7, "RUS", 63, "#27F4D2", "+11.900", "HARD", 20),
                        DriverTimingRow(8, "PER", 11, "#3671C6", "+14.320", "MEDIUM", 12),
                        DriverTimingRow(9, "ALO", 14, "#229971", "+18.450", "HARD", 25),
                        DriverTimingRow(10, "TSU", 22, "#6692FF", "+24.110", "MEDIUM", 15)
                    )
                    _state.update { it.copy(rows = timingList, isLive = true) }
                }
                delay(3000)
            }
        }
        pollingJob = job
        return job
    }

    fun stopPollingTiming() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun updateFlag(newFlag: String) {
        _state.update { it.copy(flagStatus = newFlag) }
    }
}
