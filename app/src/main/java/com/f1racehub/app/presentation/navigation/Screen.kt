package com.f1racehub.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "Этап", Icons.Filled.Home)
    data object Timing : Screen("timing", "Тайминг", Icons.AutoMirrored.Filled.List)
    data object TrackMap : Screen("trackmap", "Трек", Icons.Filled.Place)
    data object Standings : Screen("standings", "Зачет", Icons.Filled.Star)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Dashboard, Timing, TrackMap, Standings)
    }
}
