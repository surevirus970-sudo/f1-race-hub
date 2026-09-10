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
import java.time.Instant

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

            val upcomingSession = remember(gp.sessions) {
                gp.sessions.firstOrNull {
                    it.startTime.toInstant().isAfter(Instant.now())
                } ?: gp.sessions.firstOrNull()
            }

            upcomingSession?.let { session ->
                CountdownTimerCard(
                    targetTime = session.startTime.toInstant(),
                    sessionName = session.type.name,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Text(
                text = "РАСПИСАНИЕ УИКЕНДА",
                color = F1TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
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
