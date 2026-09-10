package com.f1racehub.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = F1RedPrimary,
    background = F1Background,
    surface = F1Surface,
    onPrimary = F1TextWhite,
    onBackground = F1TextWhite,
    onSurface = F1TextWhite
)

@Composable
fun F1Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
