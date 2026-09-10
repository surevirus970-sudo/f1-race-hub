package com.f1racehub.app.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1racehub.app.core.alarms.SessionAlarmScheduler
import com.f1racehub.app.domain.model.GrandPrix
import com.f1racehub.app.domain.model.RaceSession
import com.f1racehub.app.domain.repository.RaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val nextGrandPrix: GrandPrix? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class DashboardViewModel(
    private val raceRepository: RaceRepository,
    private val alarmScheduler: SessionAlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            raceRepository.observeGrandPrixList().collect { races ->
                val upcoming = races.firstOrNull { !it.isCompleted }
                _uiState.update { it.copy(nextGrandPrix = upcoming, isLoading = false) }
            }
        }
        viewModelScope.launch {
            raceRepository.refreshSchedule()
        }
    }

    fun toggleAlarm(session: RaceSession, raceName: String) {
        val triggerMillis = session.startTime.toInstant().toEpochMilli() - 15 * 60 * 1000
        viewModelScope.launch {
            if (session.isAlarmSet) {
                alarmScheduler.cancelAlarm(session.id)
                raceRepository.updateSessionAlarm(session.id, false)
            } else {
                alarmScheduler.scheduleAlarm(session.id, raceName, session.type.name, triggerMillis)
                raceRepository.updateSessionAlarm(session.id, true)
            }
        }
    }
}
