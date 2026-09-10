package com.f1racehub.app.presentation.screens.standings

import android.content.Context
import com.f1racehub.app.data.local.F1Database
import com.f1racehub.app.data.local.dao.RaceDao
import com.f1racehub.app.di.appModule
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class StandingsViewModelTest {

    @Test
    fun `initial state emits tab 0 with valid drivers and constructors lists`() {
        val viewModel = StandingsViewModel()
        val state = viewModel.state.value

        assertEquals(0, state.selectedTab, "Initial selected tab must be 0 (Drivers)")
        assertFalse(state.isLoading, "Initial loading flag must be false")
        assertEquals(6, state.drivers.size, "Initial drivers list must contain top 6 drivers")
        assertEquals(4, state.constructors.size, "Initial constructors list must contain top 4 teams")

        assertEquals("Max Verstappen", state.drivers[0].name)
        assertEquals("VER", state.drivers[0].code)
        assertEquals("McLaren", state.constructors[0].teamName)
    }

    @Test
    fun `selectTab switches between tab 0 and tab 1 cleanly`() {
        val viewModel = StandingsViewModel()

        viewModel.selectTab(1)
        assertEquals(1, viewModel.state.value.selectedTab, "Selected tab must switch to 1 (Constructors)")

        viewModel.selectTab(0)
        assertEquals(0, viewModel.state.value.selectedTab, "Selected tab must switch back to 0 (Drivers)")
    }

    @Test
    fun `standings data integrity verifies positions, non-negative points, driver codes and URLs`() {
        val viewModel = StandingsViewModel()
        val state = viewModel.state.value

        val hexPattern = Regex("^#[0-9A-Fa-f]{6}$")

        state.drivers.forEach { driver ->
            assertTrue(driver.position >= 1, "Driver position must be >= 1, found ${driver.position}")
            assertTrue(driver.points >= 0.0, "Driver points must be non-negative, found ${driver.points}")
            assertTrue(driver.name.isNotBlank(), "Driver name cannot be blank")
            assertTrue(driver.code.isNotBlank(), "Driver code cannot be blank")
            assertTrue(driver.team.isNotBlank(), "Driver team cannot be blank")
            assertTrue(driver.photoUrl.isNotBlank() && driver.photoUrl.startsWith("https://"), "Driver photoUrl must be a valid HTTPS URL")
        }

        state.constructors.forEach { constructor ->
            assertTrue(constructor.position >= 1, "Constructor position must be >= 1, found ${constructor.position}")
            assertTrue(constructor.points >= 0.0, "Constructor points must be non-negative, found ${constructor.points}")
            assertTrue(constructor.teamName.isNotBlank(), "Constructor teamName cannot be blank")
            assertTrue(constructor.teamColorHex.matches(hexPattern), "Team color hex must match #RRGGBB format: ${constructor.teamColorHex}")
        }
    }

    @Test
    fun `updateDrivers and updateConstructors update StateFlow reactively`() {
        val viewModel = StandingsViewModel()

        val customDrivers = listOf(
            DriverStandingUiModel(
                position = 1,
                name = "George Russell",
                code = "RUS",
                team = "Mercedes",
                points = 143.0,
                photoUrl = "https://media.formula1.com/d_driver_fallback_image.png/content/dam/fom-website/drivers/G/GEORUS01_George_Russell/georus01.png"
            )
        )
        viewModel.updateDrivers(customDrivers)
        assertEquals(customDrivers, viewModel.state.value.drivers, "Drivers list must reactively update")

        val customConstructors = listOf(
            ConstructorStandingUiModel(
                position = 1,
                teamName = "Aston Martin",
                points = 86.0,
                teamColorHex = "#229971"
            )
        )
        viewModel.updateConstructors(customConstructors)
        assertEquals(customConstructors, viewModel.state.value.constructors, "Constructors list must reactively update")
    }

    @Test
    fun `koin resolves StandingsViewModel from appModule`() {
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
            val resolvedVm = koinApp.koin.get<StandingsViewModel>()
            assertNotNull(resolvedVm, "StandingsViewModel must resolve cleanly from appModule")
        } finally {
            koinApp.close()
        }
    }
}
