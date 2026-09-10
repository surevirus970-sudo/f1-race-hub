package com.f1racehub.app.presentation.screens.timing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1racehub.app.presentation.components.F1PirelliTyreBadge
import com.f1racehub.app.presentation.components.RaceControlBanner
import com.f1racehub.app.presentation.theme.*

@Composable
fun LiveTimingScreen(viewModel: LiveTimingViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(F1Background)
            .padding(16.dp)
    ) {
        Text(
            text = "LIVE TIMING",
            color = F1RedPrimary,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        RaceControlBanner(
            status = state.flagStatus,
            message = "TRACK CLEAR",
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(state.rows, key = { it.driverNumber }) { row ->
                val teamColor = runCatching { Color(android.graphics.Color.parseColor(row.teamColorHex)) }.getOrDefault(Color.Gray)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(F1Surface, RoundedCornerShape(6.dp))
                        .border(1.dp, F1SurfaceBorder, RoundedCornerShape(6.dp))
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = "P${row.position}",
                        color = F1TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.width(36.dp)
                    )
                    Box(modifier = Modifier.width(4.dp).height(20.dp).background(teamColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = row.driverCode,
                        color = F1TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.width(44.dp)
                    )
                    F1PirelliTyreBadge(compound = row.tyreCompound, lapsUsed = row.tyreLaps)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = row.gapToLeader,
                        color = if (row.gapToLeader == "LEADER") F1RedPrimary else F1TextWhite,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
