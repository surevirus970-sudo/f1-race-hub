package com.f1racehub.app.data.remote

import com.f1racehub.app.data.remote.jolpica.*
import com.f1racehub.app.data.remote.openf1.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class F1ApiClientTest {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `fetchCurrentSeasonRaces should parse complete Jolpica race calendar with sessions`() = runBlocking {
        val sampleJson = """
        {
            "MRData": {
                "RaceTable": {
                    "season": "2026",
                    "Races": [
                        {
                            "season": "2026",
                            "round": "1",
                            "raceName": "Bahrain Grand Prix",
                            "Circuit": {
                                "circuitId": "bahrain",
                                "circuitName": "Bahrain International Circuit",
                                "Location": {
                                    "locality": "Sakhir",
                                    "country": "Bahrain"
                                }
                            },
                            "date": "2026-03-01",
                            "time": "15:00:00Z",
                            "FirstPractice": { "date": "2026-02-27", "time": "11:30:00Z" },
                            "SecondPractice": { "date": "2026-02-27", "time": "15:00:00Z" },
                            "ThirdPractice": { "date": "2026-02-28", "time": "12:30:00Z" },
                            "Qualifying": { "date": "2026-02-28", "time": "16:00:00Z" }
                        },
                        {
                            "season": "2026",
                            "round": "2",
                            "raceName": "Chinese Grand Prix",
                            "Circuit": {
                                "circuitId": "shanghai",
                                "circuitName": "Shanghai International Circuit",
                                "Location": {
                                    "locality": "Shanghai",
                                    "country": "China"
                                }
                            },
                            "date": "2026-03-22",
                            "time": "07:00:00Z",
                            "FirstPractice": { "date": "2026-03-20", "time": "03:30:00Z" },
                            "Qualifying": { "date": "2026-03-21", "time": "07:00:00Z" },
                            "Sprint": { "date": "2026-03-21", "time": "03:00:00Z" }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("/ergast/f1/current.json", request.url.encodedPath)
            respond(
                content = sampleJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(jsonConfig) }
        }

        val client = F1ApiClient(httpClient)
        val races = client.fetchCurrentSeasonRaces()

        assertEquals(2, races.size)

        val race1 = races[0]
        assertEquals("2026", race1.season)
        assertEquals("1", race1.round)
        assertEquals("Bahrain Grand Prix", race1.raceName)
        assertEquals("bahrain", race1.circuit.circuitId)
        assertEquals("Bahrain International Circuit", race1.circuit.circuitName)
        assertEquals("Sakhir", race1.circuit.location.locality)
        assertEquals("Bahrain", race1.circuit.location.country)
        assertEquals("2026-03-01", race1.date)
        assertEquals("15:00:00Z", race1.time)
        assertNotNull(race1.firstPractice)
        assertEquals("2026-02-27", race1.firstPractice?.date)
        assertEquals("11:30:00Z", race1.firstPractice?.time)
        assertNotNull(race1.secondPractice)
        assertNotNull(race1.thirdPractice)
        assertNotNull(race1.qualifying)
        assertNull(race1.sprint)

        val race2 = races[1]
        assertEquals("2", race2.round)
        assertEquals("Chinese Grand Prix", race2.raceName)
        assertNotNull(race2.sprint)
        assertEquals("2026-03-21", race2.sprint?.date)
        assertEquals("03:00:00Z", race2.sprint?.time)
        assertNull(race2.secondPractice)
        assertNull(race2.thirdPractice)
    }

    @Test
    fun `fetchCurrentSeasonRaces should handle nullable and missing session times`() = runBlocking {
        val sampleJson = """
        {
            "MRData": {
                "RaceTable": {
                    "season": "2026",
                    "Races": [
                        {
                            "season": "2026",
                            "round": "24",
                            "raceName": "Abu Dhabi Grand Prix",
                            "Circuit": {
                                "circuitId": "yas_marina",
                                "circuitName": "Yas Marina Circuit",
                                "Location": {
                                    "locality": "Abu Dhabi",
                                    "country": "UAE"
                                }
                            },
                            "date": "2026-12-06",
                            "FirstPractice": { "date": "2026-12-04" }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = sampleJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(jsonConfig) }
        }

        val client = F1ApiClient(httpClient)
        val races = client.fetchCurrentSeasonRaces()

        assertEquals(1, races.size)
        val race = races[0]
        assertNull(race.time)
        assertNull(race.sprint)
        assertNull(race.secondPractice)
        assertNull(race.thirdPractice)
        assertNotNull(race.firstPractice)
        assertEquals("2026-12-04", race.firstPractice?.date)
        assertNull(race.firstPractice?.time)
    }

    @Test
    fun `fetchLiveIntervals should query session_key and deserialize intervals with nullable gapToLeader`() = runBlocking {
        val sampleJson = """
        [
            {
                "driver_number": 1,
                "gap_to_leader": null,
                "interval": null,
                "date": "2026-05-24T14:10:00.123Z"
            },
            {
                "driver_number": 16,
                "gap_to_leader": 1.450,
                "interval": 1.450,
                "date": "2026-05-24T14:10:00.150Z"
            },
            {
                "driver_number": 4,
                "gap_to_leader": 3.820,
                "interval": 2.370,
                "date": "2026-05-24T14:10:00.180Z"
            }
        ]
        """.trimIndent()

        var capturedSessionKey: String? = null
        val mockEngine = MockEngine { request ->
            assertEquals("/v1/intervals", request.url.encodedPath)
            capturedSessionKey = request.url.parameters["session_key"]
            respond(
                content = sampleJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(jsonConfig) }
        }

        val client = F1ApiClient(httpClient)
        val intervals = client.fetchLiveIntervals("9158")

        assertEquals("9158", capturedSessionKey)
        assertEquals(3, intervals.size)

        val leader = intervals[0]
        assertEquals(1, leader.driverNumber)
        assertNull(leader.gapToLeader)
        assertNull(leader.interval)
        assertEquals("2026-05-24T14:10:00.123Z", leader.date)

        val p2 = intervals[1]
        assertEquals(16, p2.driverNumber)
        assertEquals(1.450, p2.gapToLeader)
        assertEquals(1.450, p2.interval)
        assertEquals("2026-05-24T14:10:00.150Z", p2.date)

        val p3 = intervals[2]
        assertEquals(4, p3.driverNumber)
        assertEquals(3.820, p3.gapToLeader)
        assertEquals(2.370, p3.interval)
    }

    @Test
    fun `fetchCarLocations should query session_key and parse 3D coordinates`() = runBlocking {
        val sampleJson = """
        [
            {
                "driver_number": 1,
                "date": "2026-05-24T14:10:05.000Z",
                "x": 1420.5,
                "y": -2380.7,
                "z": 15.2
            },
            {
                "driver_number": 44,
                "date": "2026-05-24T14:10:05.100Z",
                "x": 1390.2,
                "y": -2395.1,
                "z": 15.0
            }
        ]
        """.trimIndent()

        var capturedSessionKey: String? = null
        val mockEngine = MockEngine { request ->
            assertEquals("/v1/location", request.url.encodedPath)
            capturedSessionKey = request.url.parameters["session_key"]
            respond(
                content = sampleJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(jsonConfig) }
        }

        val client = F1ApiClient(httpClient)
        val locations = client.fetchCarLocations("9158")

        assertEquals("9158", capturedSessionKey)
        assertEquals(2, locations.size)

        val car1 = locations[0]
        assertEquals(1, car1.driverNumber)
        assertEquals("2026-05-24T14:10:05.000Z", car1.date)
        assertEquals(1420.5, car1.x)
        assertEquals(-2380.7, car1.y)
        assertEquals(15.2, car1.z)

        val car2 = locations[1]
        assertEquals(44, car2.driverNumber)
        assertEquals("2026-05-24T14:10:05.100Z", car2.date)
        assertEquals(1390.2, car2.x)
        assertEquals(-2395.1, car2.y)
        assertEquals(15.0, car2.z)
    }

    @Test
    fun `OpenF1StintDto should deserialize tyre compound and default tyre age`() {
        val jsonWithAge = """{"driver_number":81,"stint_number":1,"compound":"HARD","tyre_age_at_start":12}"""
        val stintWithAge = jsonConfig.decodeFromString<OpenF1StintDto>(jsonWithAge)
        assertEquals(81, stintWithAge.driverNumber)
        assertEquals(1, stintWithAge.stintNumber)
        assertEquals("HARD", stintWithAge.compound)
        assertEquals(12, stintWithAge.tyreAgeAtStart)

        val jsonWithoutAge = """{"driver_number":81,"stint_number":2,"compound":"SOFT"}"""
        val stintWithoutAge = jsonConfig.decodeFromString<OpenF1StintDto>(jsonWithoutAge)
        assertEquals(81, stintWithoutAge.driverNumber)
        assertEquals(2, stintWithoutAge.stintNumber)
        assertEquals("SOFT", stintWithoutAge.compound)
        assertEquals(0, stintWithoutAge.tyreAgeAtStart)
    }
}
