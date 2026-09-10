package com.f1racehub.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1racehub.app.core.time.CountdownRemaining
import com.f1racehub.app.core.time.DateTimeFormatterUtil
import com.f1racehub.app.presentation.theme.*
import kotlinx.coroutines.delay
import java.time.Instant

@Composable
fun CountdownTimerCard(targetTime: Instant, sessionName: String, modifier: Modifier = Modifier) {
    var remaining by remember(targetTime) {
        mutableStateOf(DateTimeFormatterUtil.calculateRemainingDuration(targetTime))
    }

    LaunchedEffect(targetTime) {
        while (!remaining.isExpired) {
            delay(1000)
            remaining = DateTimeFormatterUtil.calculateRemainingDuration(targetTime)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = F1Surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, F1SurfaceBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = sessionName.uppercase(), color = F1RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (remaining.isExpired) {
                Text(text = "СЕССИЯ ИДЕТ", color = F1TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Black)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeUnitBox(value = remaining.days, label = "ДНЕЙ")
                    TimeUnitBox(value = remaining.hours, label = "ЧАСОВ")
                    TimeUnitBox(value = remaining.minutes, label = "МИН")
                    TimeUnitBox(value = remaining.seconds, label = "СЕК")
                }
            }
        }
    }
}

@Composable
private fun TimeUnitBox(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = String.format("%02d", value),
            color = F1TextWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = label, color = F1TextMuted, fontSize = 10.sp)
    }
}
