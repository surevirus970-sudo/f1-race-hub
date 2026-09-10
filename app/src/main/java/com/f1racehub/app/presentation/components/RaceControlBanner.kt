package com.f1racehub.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1racehub.app.presentation.theme.*

@Composable
fun RaceControlBanner(status: String, message: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "GREEN" -> FlagGreen to Color.Black
        "YELLOW", "DOUBLE_YELLOW" -> FlagYellow to Color.Black
        "RED" -> FlagRed to Color.White
        else -> F1Surface to F1TextWhite
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = status.uppercase(),
            color = textColor,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            color = textColor,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}
