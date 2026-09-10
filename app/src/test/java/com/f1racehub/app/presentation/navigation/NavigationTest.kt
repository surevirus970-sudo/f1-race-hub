package com.f1racehub.app.presentation.navigation

import android.content.Context
import com.f1racehub.app.MainActivity
import com.f1racehub.app.data.local.F1Database
import com.f1racehub.app.data.local.dao.RaceDao
import com.f1racehub.app.di.appModule
import com.f1racehub.app.presentation.screens.dashboard.DashboardViewModel
import com.f1racehub.app.presentation.screens.standings.StandingsViewModel
import com.f1racehub.app.presentation.screens.timing.LiveTimingViewModel
import com.f1racehub.app.presentation.screens.trackmap.TrackMapViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationTest {

    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `screen destinations define expected routes and titles`() {
        assertEquals("dashboard", Screen.Dashboard.route)
        assertEquals("Этап", Screen.Dashboard.title)

        assertEquals("timing", Screen.Timing.route)
        assertEquals("Тайминг", Screen.Timing.title)

        assertEquals("trackmap", Screen.TrackMap.route)
        assertEquals("Трек", Screen.TrackMap.title)

        assertEquals("standings", Screen.Standings.route)
        assertEquals("Зачет", Screen.Standings.title)

        assertEquals(4, Screen.bottomNavItems.size)
        assertEquals(
            listOf(Screen.Dashboard, Screen.Timing, Screen.TrackMap, Screen.Standings),
            Screen.bottomNavItems
        )
    }

    @Test
    fun `all 4 screen viewmodels resolve cleanly from koin appModule`() {
        val mockContext = mockk<Context>(relaxed = true)
        val mockDatabase = mockk<F1Database>()
        val mockDao = mockk<RaceDao>()
        every { mockDatabase.raceDao() } returns mockDao

        val koinApp = koinApplication {
            androidContext(mockContext)
            allowOverride(true)
            modules(
                appModule,
                module {
                    single<F1Database> { mockDatabase }
                }
            )
        }

        try {
            val dashboardVm = koinApp.koin.get<DashboardViewModel>()
            assertNotNull(dashboardVm, "DashboardViewModel must resolve from appModule")

            val timingVm = koinApp.koin.get<LiveTimingViewModel>()
            assertNotNull(timingVm, "LiveTimingViewModel must resolve from appModule")

            val trackMapVm = koinApp.koin.get<TrackMapViewModel>()
            assertNotNull(trackMapVm, "TrackMapViewModel must resolve from appModule")

            val standingsVm = koinApp.koin.get<StandingsViewModel>()
            assertNotNull(standingsVm, "StandingsViewModel must resolve from appModule")
        } finally {
            koinApp.close()
        }
    }

    @Test
    fun `mainActivity extends ComponentActivity`() {
        assertTrue(
            androidx.activity.ComponentActivity::class.java.isAssignableFrom(MainActivity::class.java),
            "MainActivity must inherit from ComponentActivity"
        )
    }
}
