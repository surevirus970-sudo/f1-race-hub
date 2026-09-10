package com.f1racehub.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
fun F1PirelliTyreBadge(compound: String, lapsUsed: Int, modifier: Modifier = Modifier) {
    val (color, letter) = when (compound.uppercase()) {
        "SOFT" -> TyreSoft to "S"
        "MEDIUM" -> TyreMedium to "M"
        "HARD" -> TyreHard to "H"
        "INTERMEDIATE" -> TyreInter to "I"
        "WET" -> TyreWet to "W"
        else -> Color.Gray to "?"
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .background(Color.Black, CircleShape)
                .border(2.dp, color, CircleShape)
        ) {
            Text(text = letter, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "${lapsUsed}L", color = F1TextMuted, fontSize = 12.sp)
    }
}
