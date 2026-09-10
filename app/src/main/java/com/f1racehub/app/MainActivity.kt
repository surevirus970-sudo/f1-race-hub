package com.f1racehub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.f1racehub.app.presentation.navigation.Screen
import com.f1racehub.app.presentation.screens.dashboard.DashboardScreen
import com.f1racehub.app.presentation.screens.dashboard.DashboardViewModel
import com.f1racehub.app.presentation.screens.standings.StandingsScreen
import com.f1racehub.app.presentation.screens.standings.StandingsViewModel
import com.f1racehub.app.presentation.screens.timing.LiveTimingScreen
import com.f1racehub.app.presentation.screens.timing.LiveTimingViewModel
import com.f1racehub.app.presentation.screens.trackmap.TrackMapScreen
import com.f1racehub.app.presentation.screens.trackmap.TrackMapViewModel
import com.f1racehub.app.presentation.theme.F1Background
import com.f1racehub.app.presentation.theme.F1RedPrimary
import com.f1racehub.app.presentation.theme.F1Surface
import com.f1racehub.app.presentation.theme.F1TextMuted
import com.f1racehub.app.presentation.theme.F1Theme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            F1Theme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(containerColor = F1Surface) {
                            Screen.bottomNavItems.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title) },
                                    selected = selected,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = F1RedPrimary,
                                        selectedTextColor = F1RedPrimary,
                                        indicatorColor = F1Background,
                                        unselectedIconColor = F1TextMuted,
                                        unselectedTextColor = F1TextMuted
                                    ),
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Dashboard.route) {
                            val vm: DashboardViewModel = koinViewModel()
                            DashboardScreen(vm)
                        }
                        composable(Screen.Timing.route) {
                            val vm: LiveTimingViewModel = koinViewModel()
                            LiveTimingScreen(vm)
                        }
                        composable(Screen.TrackMap.route) {
                            val vm: TrackMapViewModel = koinViewModel()
                            TrackMapScreen(vm)
                        }
                        composable(Screen.Standings.route) {
                            val vm: StandingsViewModel = koinViewModel()
                            StandingsScreen(vm)
                        }
                    }
                }
            }
        }
    }
}
