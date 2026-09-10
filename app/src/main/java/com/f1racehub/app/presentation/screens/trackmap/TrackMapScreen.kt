package com.f1racehub.app.presentation.screens.trackmap

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1racehub.app.presentation.theme.F1Background
import com.f1racehub.app.presentation.theme.F1RedPrimary
import com.f1racehub.app.presentation.theme.F1Surface
import com.f1racehub.app.presentation.theme.F1SurfaceBorder
import com.f1racehub.app.presentation.theme.F1TextMuted
import com.f1racehub.app.presentation.theme.F1TextWhite
import com.f1racehub.app.presentation.theme.FlagGreen

@Composable
fun TrackMapScreen(viewModel: TrackMapViewModel) {
    val state by viewModel.state.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(F1Background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "КАРТА ТРАССЫ",
                    color = F1RedPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.circuitName,
                    color = F1TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Live pulse badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(F1Surface, RoundedCornerShape(16.dp))
                    .border(1.dp, F1SurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(FlagGreen.copy(alpha = pulseAlpha), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE",
                    color = F1TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Track Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = F1RedPrimary)
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pad = 36.dp.toPx()
                    val w = size.width - 2 * pad
                    val h = size.height - 2 * pad

                    if (w > 0 && h > 0) {
                        // Asphalt track surface
                        drawOval(
                            color = Color(0xFF23232C),
                            topLeft = Offset(pad, pad),
                            size = Size(w, h),
                            style = Stroke(width = 16.dp.toPx())
                        )

                        // Racing line accent
                        drawOval(
                            color = Color(0xFF3F3F4E),
                            topLeft = Offset(pad, pad),
                            size = Size(w, h),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Start / finish sector line at apex
                        val finishX = pad + w * 0.5f
                        val finishY = pad
                        drawLine(
                            color = Color.White,
                            start = Offset(finishX, finishY - 8.dp.toPx()),
                            end = Offset(finishX, finishY + 8.dp.toPx()),
                            strokeWidth = 3.dp.toPx()
                        )

                        // Dynamic car markers
                        state.cars.forEach { car ->
                            val cx = pad + car.normX * w
                            val cy = pad + car.normY * h
                            val carColor = runCatching {
                                Color(android.graphics.Color.parseColor(car.teamColorHex))
                            }.getOrDefault(Color.White)

                            // Outer halo / border circle
                            drawCircle(
                                color = Color.White,
                                radius = 12.dp.toPx(),
                                center = Offset(cx, cy)
                            )

                            // Inner team-colored circle
                            drawCircle(
                                color = carColor,
                                radius = 10.dp.toPx(),
                                center = Offset(cx, cy)
                            )

                            // Central telemetry dot
                            drawCircle(
                                color = Color(0xFF101014),
                                radius = 3.dp.toPx(),
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom legend card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = F1Surface),
            border = BorderStroke(1.dp, F1SurfaceBorder)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "ПЕЛОТОН НА ТРАССЕ",
                    color = F1TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.cars.forEach { car ->
                        val teamColor = runCatching {
                            Color(android.graphics.Color.parseColor(car.teamColorHex))
                        }.getOrDefault(Color.White)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFF23232C), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(teamColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${car.driverCode} #${car.driverNumber}",
                                color = F1TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
