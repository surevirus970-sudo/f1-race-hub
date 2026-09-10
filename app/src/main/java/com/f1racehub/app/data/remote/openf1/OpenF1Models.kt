package com.f1racehub.app.data.remote.openf1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenF1IntervalDto(
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("gap_to_leader") val gapToLeader: Double? = null,
    val interval: Double? = null,
    val date: String
)

@Serializable
data class OpenF1StintDto(
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("stint_number") val stintNumber: Int,
    val compound: String, // SOFT, MEDIUM, HARD, INTERMEDIATE, WET
    @SerialName("tyre_age_at_start") val tyreAgeAtStart: Int = 0
)

@Serializable
data class OpenF1LocationDto(
    @SerialName("driver_number") val driverNumber: Int,
    val date: String,
    val x: Double,
    val y: Double,
    val z: Double
)
