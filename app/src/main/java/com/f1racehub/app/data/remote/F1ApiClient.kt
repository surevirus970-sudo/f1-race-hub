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

    suspend fun fetchStints(sessionKey: String): List<OpenF1StintDto> {
        return httpClient.get("$openF1BaseUrl/stints") {
            parameter("session_key", sessionKey)
        }.body()
    }
}
