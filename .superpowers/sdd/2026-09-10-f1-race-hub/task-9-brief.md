# Task 9 Brief: Dashboard Screen & Next Grand Prix Timeline

## Files to Create
- `app/src/main/java/com/f1racehub/app/presentation/screens/dashboard/DashboardViewModel.kt`
- `app/src/main/java/com/f1racehub/app/presentation/screens/dashboard/DashboardScreen.kt`
- `app/src/test/java/com/f1racehub/app/presentation/screens/dashboard/DashboardViewModelTest.kt`

## Interfaces
- Consumes: `RaceRepository`, `SessionAlarmScheduler`, `DateTimeFormatterUtil`, `F1Theme` components (`CountdownTimerCard`, `F1Colors`).
- Produces:
  - `data class DashboardUiState(val nextGrandPrix: GrandPrix?, val isLoading: Boolean, val errorMessage: String?)`
  - `class DashboardViewModel(raceRepository: RaceRepository, alarmScheduler: SessionAlarmScheduler): ViewModel`
  - `@Composable fun DashboardScreen(viewModel: DashboardViewModel)`

## Implementation Details

### 1. `DashboardViewModel.kt`
```kotlin
package com.f1racehub.app.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1racehub.app.core.alarms.SessionAlarmScheduler
import com.f1racehub.app.domain.model.GrandPrix
import com.f1racehub.app.domain.model.RaceSession
import com.f1racehub.app.domain.repository.RaceRepository
import kotlinx.coroutines.flow.*
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
```

### 2. `DashboardScreen.kt`
```kotlin
package com.f1racehub.app.presentation.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1racehub.app.core.time.DateTimeFormatterUtil
import com.f1racehub.app.presentation.components.CountdownTimerCard
import com.f1racehub.app.presentation.theme.*

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(F1Background)
            .padding(16.dp)
    ) {
        Text(
            text = "F1 RACE HUB",
            color = F1RedPrimary,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        state.nextGrandPrix?.let { gp ->
            Card(
                colors = CardDefaults.cardColors(containerColor = F1Surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, F1SurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "СЛЕДУЮЩИЙ ЭТАП • РАУНД ${gp.round}", color = F1RedPrimary, fontSize = 12.sp)
                    Text(text = gp.name, color = F1TextWhite, style = MaterialTheme.typography.headlineSmall)
                    Text(text = "📍 ${gp.circuitName}, ${gp.country}", color = F1TextMuted, fontSize = 14.sp)
                }
            }

            gp.sessions.firstOrNull()?.let { nextSession ->
                CountdownTimerCard(
                    targetTime = nextSession.startTime.toInstant(),
                    sessionName = nextSession.type.name,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Text(
                text = "РАСПИСАНИЕ УИКЕНДА",
                color = F1TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(gp.sessions, key = { it.id }) { session ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(F1Surface)
                            .border(1.dp, F1SurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = session.type.name, color = F1TextWhite, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = DateTimeFormatterUtil.formatToUserFriendlyTime(session.startTime),
                                color = F1TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { viewModel.toggleAlarm(session, gp.name) }) {
                            Icon(
                                imageVector = if (session.isAlarmSet) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "Будильник",
                                tint = if (session.isAlarmSet) F1RedPrimary else F1TextMuted
                            )
                        }
                    }
                }
            }
        } ?: run {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(color = F1RedPrimary)
            }
        }
    }
}
```

### 3. Unit Test `DashboardViewModelTest.kt`
Unit test testing ViewModel with MockK / Turbine:
- Initial state emissions: loading state -> loaded state with upcoming race.
- `toggleAlarm()` setting alarm when `isAlarmSet == false` and updating repository.
- `toggleAlarm()` cancelling alarm when `isAlarmSet == true` and updating repository.

### 4. Commit
Commit with message: `feat(ui): implement DashboardScreen and DashboardViewModel with countdown and alarms`
