package com.f1racehub.app.data.remote.jolpica

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JolpicaResponse<T>(
    @SerialName("MRData") val mrData: T
)

@Serializable
data class RaceTableWrapper(
    @SerialName("RaceTable") val raceTable: RaceTableData
)

@Serializable
data class RaceTableData(
    val season: String,
    @SerialName("Races") val races: List<JolpicaRaceDto>
)

@Serializable
data class JolpicaRaceDto(
    val season: String,
    val round: String,
    val raceName: String,
    @SerialName("Circuit") val circuit: CircuitDto,
    val date: String,
    val time: String? = null,
    @SerialName("FirstPractice") val firstPractice: SessionTimeDto? = null,
    @SerialName("SecondPractice") val secondPractice: SessionTimeDto? = null,
    @SerialName("ThirdPractice") val thirdPractice: SessionTimeDto? = null,
    @SerialName("Qualifying") val qualifying: SessionTimeDto? = null,
    @SerialName("Sprint") val sprint: SessionTimeDto? = null
)

@Serializable
data class CircuitDto(
    val circuitId: String,
    val circuitName: String,
    @SerialName("Location") val location: LocationDetailsDto
)

@Serializable
data class LocationDetailsDto(
    val locality: String,
    val country: String
)

@Serializable
data class SessionTimeDto(
    val date: String,
    val time: String? = null
)
