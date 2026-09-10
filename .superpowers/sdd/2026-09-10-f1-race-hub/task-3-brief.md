# Task 3 Brief: Jolpica & OpenF1 Remote API DTOs and Ktor Client (TDD)

## Files to Create
- `app/src/main/java/com/f1racehub/app/data/remote/jolpica/JolpicaModels.kt`
- `app/src/main/java/com/f1racehub/app/data/remote/openf1/OpenF1Models.kt`
- `app/src/main/java/com/f1racehub/app/data/remote/F1ApiClient.kt`
- `app/src/test/java/com/f1racehub/app/data/remote/F1ApiClientTest.kt`

## Interfaces
- Consumes: Ktor Client (`HttpClient`, `io.ktor.client.call.body`, `io.ktor.client.request.get`).
- Produces:
  - `fetchCurrentSeasonRaces(): List<JolpicaRaceDto>`
  - `fetchLiveIntervals(sessionKey: String): List<OpenF1IntervalDto>`
  - `fetchCarLocations(sessionKey: String): List<OpenF1LocationDto>`

## Implementation Details

### 1. `app/src/main/java/com/f1racehub/app/data/remote/jolpica/JolpicaModels.kt`
```kotlin
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
```

### 2. `app/src/main/java/com/f1racehub/app/data/remote/openf1/OpenF1Models.kt`
```kotlin
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
```

### 3. `app/src/main/java/com/f1racehub/app/data/remote/F1ApiClient.kt`
```kotlin
package com.f1racehub.app.data.remote

import com.f1racehub.app.data.remote.jolpica.*
import com.f1racehub.app.data.remote.openf1.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class F1ApiClient(
    private val httpClient: HttpClient,
    private val jolpicaBaseUrl: String = "https://api.jolpica.com/ergast/f1",
    private val openF1BaseUrl: String = "https://api.openf1.org/v1"
) {
    suspend fun fetchCurrentSeasonRaces(): List<JolpicaRaceDto> {
        val response: JolpicaResponse<RaceTableWrapper> = httpClient.get("$jolpicaBaseUrl/current.json").body()
        return response.mrData.raceTable.races
    }

    suspend fun fetchLiveIntervals(sessionKey: String): List<OpenF1IntervalDto> {
        return httpClient.get("$openF1BaseUrl/intervals") {
            parameter("session_key", sessionKey)
        }.body()
    }

    suspend fun fetchCarLocations(sessionKey: String): List<OpenF1LocationDto> {
        return httpClient.get("$openF1BaseUrl/location") {
            parameter("session_key", sessionKey)
        }.body()
    }
}
```

### 4. Unit Test `app/src/test/java/com/f1racehub/app/data/remote/F1ApiClientTest.kt`
Write unit tests validating JSON deserialization with real sample payloads for Jolpica race calendar, OpenF1 intervals, and OpenF1 locations using `MockEngine` or MockK.
Verify deserialization of nullable fields (`gapToLeader`, `time`, `sprint`).

### 5. Commit
Commit with message: `feat(network): add DTO models and F1ApiClient with unit tests`
