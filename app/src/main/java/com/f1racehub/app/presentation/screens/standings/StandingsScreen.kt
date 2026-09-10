package com.f1racehub.app.presentation.screens.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.f1racehub.app.presentation.theme.F1Background
import com.f1racehub.app.presentation.theme.F1RedPrimary
import com.f1racehub.app.presentation.theme.F1Surface
import com.f1racehub.app.presentation.theme.F1SurfaceBorder
import com.f1racehub.app.presentation.theme.F1TextMuted
import com.f1racehub.app.presentation.theme.F1TextWhite

@Composable
fun StandingsScreen(viewModel: StandingsViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(F1Background)
            .padding(16.dp)
    ) {
        // Screen Header
        Text(
            text = "ТАБЛИЦЫ ЧЕМПИОНАТА",
            color = F1RedPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Material 3 Navigation TabRow
        TabRow(
            selectedTabIndex = state.selectedTab,
            containerColor = F1Surface,
            contentColor = F1RedPrimary,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = state.selectedTab == 0,
                onClick = { viewModel.selectTab(0) }
            ) {
                Text(
                    text = "ПИЛОТЫ",
                    color = if (state.selectedTab == 0) F1RedPrimary else F1TextMuted,
                    fontWeight = if (state.selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Tab(
                selected = state.selectedTab == 1,
                onClick = { viewModel.selectTab(1) }
            ) {
                Text(
                    text = "КОМАНДЫ",
                    color = if (state.selectedTab == 1) F1RedPrimary else F1TextMuted,
                    fontWeight = if (state.selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = F1RedPrimary)
            }
        } else {
            when (state.selectedTab) {
                0 -> DriversStandingsList(drivers = state.drivers, modifier = Modifier.weight(1f))
                1 -> ConstructorsStandingsList(constructors = state.constructors, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DriversStandingsList(
    drivers: List<DriverStandingUiModel>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(drivers, key = { it.code }) { driver ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(F1Surface, RoundedCornerShape(8.dp))
                    .border(1.dp, F1SurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "${driver.position}",
                    color = F1TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(32.dp)
                )

                AsyncImage(
                    model = driver.photoUrl,
                    contentDescription = driver.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF23232C))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = driver.name,
                        color = F1TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = driver.team,
                        color = F1TextMuted,
                        fontSize = 12.sp
                    )
                }

                val ptsText = if (driver.points % 1.0 == 0.0) {
                    "${driver.points.toLong()} PTS"
                } else {
                    "${driver.points} PTS"
                }

                Text(
                    text = ptsText,
                    color = F1RedPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ConstructorsStandingsList(
    constructors: List<ConstructorStandingUiModel>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(constructors, key = { it.teamName }) { constructor ->
            val teamColor = runCatching {
                Color(android.graphics.Color.parseColor(constructor.teamColorHex))
            }.getOrDefault(Color.White)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(F1Surface, RoundedCornerShape(8.dp))
                    .border(1.dp, F1SurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "${constructor.position}",
                    color = F1TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(32.dp)
                )

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .background(teamColor, RoundedCornerShape(2.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = constructor.teamName,
                    color = F1TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )

                val ptsText = if (constructor.points % 1.0 == 0.0) {
                    "${constructor.points.toLong()} PTS"
                } else {
                    "${constructor.points} PTS"
                }

                Text(
                    text = ptsText,
                    color = F1RedPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
