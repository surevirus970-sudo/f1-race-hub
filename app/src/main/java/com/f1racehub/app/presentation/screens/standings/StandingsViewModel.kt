package com.f1racehub.app.presentation.screens.standings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DriverStandingUiModel(
    val position: Int,
    val name: String,
    val code: String,
    val team: String,
    val points: Double,
    val photoUrl: String
)

data class ConstructorStandingUiModel(
    val position: Int,
    val teamName: String,
    val points: Double,
    val teamColorHex: String
)

data class StandingsUiState(
    val selectedTab: Int = 0,
    val isLoading: Boolean = false,
    val drivers: List<DriverStandingUiModel> = defaultDrivers,
    val constructors: List<ConstructorStandingUiModel> = defaultConstructors
) {
    companion object {
        val defaultDrivers = listOf(
            DriverStandingUiModel(
                position = 1,
                name = "Max Verstappen",
                code = "VER",
                team = "Red Bull Racing",
                points = 331.0,
                photoUrl = "https://media.formula1.com/d_driver_fallback_image.png/content/dam/fom-website/drivers/M/MAXVER01_Max_Verstappen/maxver01.png"
            ),
            DriverStandingUiModel(
                position = 2,
                name = "Lando Norris",
                code = "NOR",
                team = "McLaren",
                points = 279.0,
                photoUrl = "https://media.formula1.com/d_driver_fallback_image.png/content/dam/fom-website/drivers/L/LANNOR01_Lando_Norris/lannor01.png"
            ),
            DriverStandingUiModel(
                position = 3,
                name = "Charles Leclerc",
                code = "LEC",
                team = "Ferrari",
                points = 245.0,
                photoUrl = "https://media.formula1.com/d_driver_fallback_image.png/content/dam/fom-website/drivers/C/CHALEC01_Charles_Leclerc/chalec01.png"
            ),
            DriverStandingUiModel(
                position = 4,
                name = "Oscar Piastri",
                code = "PIA",
                team = "McLaren",
                points = 237.0,
                photoUrl = "https://media.formula1.com/d_driver_fallback_image.png/content/dam/fom-website/drivers/O/OSCPIA01_Oscar_Piastri/oscpia01.png"
            ),
            DriverStandingUiModel(
                position = 5,
                name = "Carlos Sainz",
                code = "SAI",
                team = "Ferrari",
                points = 190.0,
                photoUrl = "https://media.formula1.com/d_driver_fallback_image.png/content/dam/fom-website/drivers/C/CARSAI01_Carlos_Sainz/carsai01.png"
            ),
            DriverStandingUiModel(
                position = 6,
                name = "Lewis Hamilton",
                code = "HAM",
                team = "Mercedes",
                points = 174.0,
                photoUrl = "https://media.formula1.com/d_driver_fallback_image.png/content/dam/fom-website/drivers/L/LEWHAM01_Lewis_Hamilton/lewham01.png"
            )
        )

        val defaultConstructors = listOf(
            ConstructorStandingUiModel(
                position = 1,
                teamName = "McLaren",
                points = 516.0,
                teamColorHex = "#FF8000"
            ),
            ConstructorStandingUiModel(
                position = 2,
                teamName = "Red Bull Racing",
                points = 475.0,
                teamColorHex = "#3671C6"
            ),
            ConstructorStandingUiModel(
                position = 3,
                teamName = "Ferrari",
                points = 441.0,
                teamColorHex = "#E8002D"
            ),
            ConstructorStandingUiModel(
                position = 4,
                teamName = "Mercedes",
                points = 329.0,
                teamColorHex = "#27F4D2"
            )
        )
    }
}

class StandingsViewModel : ViewModel() {

    private val _state = MutableStateFlow(StandingsUiState())
    val state: StateFlow<StandingsUiState> = _state.asStateFlow()

    fun selectTab(index: Int) {
        _state.update { it.copy(selectedTab = index) }
    }

    fun updateDrivers(drivers: List<DriverStandingUiModel>) {
        _state.update { it.copy(drivers = drivers) }
    }

    fun updateConstructors(constructors: List<ConstructorStandingUiModel>) {
        _state.update { it.copy(constructors = constructors) }
    }
}
