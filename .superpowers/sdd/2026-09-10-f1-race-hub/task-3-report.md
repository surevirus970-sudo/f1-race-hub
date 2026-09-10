# Task 3 Report: Jolpica & OpenF1 Remote API DTOs and Ktor Client (TDD)

## Status
DONE

## Created and Modified Files
1. `app/src/main/java/com/f1racehub/app/data/remote/jolpica/JolpicaModels.kt`
   - Data structures for Ergast/Jolpica JSON endpoints annotated with `@Serializable` and `@SerialName`.
   - `JolpicaResponse<T>` wrapper mapping root `MRData`.
   - `RaceTableWrapper` and `RaceTableData` holding the season identifier and `Races` list.
   - `JolpicaRaceDto` encapsulating season, round, raceName, circuit metadata, race date/time, and discrete session slots (`FirstPractice`, `SecondPractice`, `ThirdPractice`, `Qualifying`, `Sprint`) with nullable timestamp handling.
   - `CircuitDto` and `LocationDetailsDto` for geographical coordinates and circuit taxonomy.
   - `SessionTimeDto` with `date` and nullable `time`.
2. `app/src/main/java/com/f1racehub/app/data/remote/openf1/OpenF1Models.kt`
   - `OpenF1IntervalDto`: serializes live telemetry timing intervals (`driver_number`, nullable `gap_to_leader`, nullable `interval`, ISO-8601 `date`).
   - `OpenF1StintDto`: tyre strategy representation (`driver_number`, `stint_number`, `compound`, default `tyre_age_at_start = 0`).
   - `OpenF1LocationDto`: 3D track coordinate telemetry (`driver_number`, `date`, `x`, `y`, `z` Double precision coordinates).
3. `app/src/main/java/com/f1racehub/app/data/remote/F1ApiClient.kt`
   - Network client wrapping Ktor `HttpClient`.
   - `fetchCurrentSeasonRaces()` queries `$jolpicaBaseUrl/current.json` and extracts `List<JolpicaRaceDto>`.
   - `fetchLiveIntervals(sessionKey: String)` queries `$openF1BaseUrl/intervals` with query parameter `session_key`.
   - `fetchCarLocations(sessionKey: String)` queries `$openF1BaseUrl/location` with query parameter `session_key`.
4. `app/src/test/java/com/f1racehub/app/data/remote/F1ApiClientTest.kt`
   - 5 unit test cases using Ktor `MockEngine` and `ContentNegotiation` with `Json { ignoreUnknownKeys = true; isLenient = true }`:
     - `fetchCurrentSeasonRaces should parse complete Jolpica race calendar with sessions`: validates two distinct race payloads (standard weekend and Sprint format), full circuit and locality nesting, and session timestamps.
     - `fetchCurrentSeasonRaces should handle nullable and missing session times`: validates missing/null `time`, `sprint`, and free practice components.
     - `fetchLiveIntervals should query session_key and deserialize intervals with nullable gapToLeader`: verifies parameter serialization and leader delta nullability (`gapToLeader = null`).
     - `fetchCarLocations should query session_key and parse 3D coordinates`: verifies query parameters and Double precision spatial coordinates.
     - `OpenF1StintDto should deserialize tyre compound and default tyre age`: verifies explicit tyre age vs default zero allocation.
5. `gradle/libs.versions.toml` & `app/build.gradle.kts`
   - Registered `ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }` in version catalog.
   - Added `testImplementation(libs.ktor.client.mock)` to application build configuration.

## Verification
- Compilation verified with `kotlinc-jvm 2.3.10` and `kotlinx-serialization-compiler-plugin` targeting JRE 25.
- Executed unit test suite covering `F1ApiClientTest` (5 tests) alongside existing `DateTimeFormatterUtilTest` (5 tests).
- 100% pass rate achieved (10 passing tests across the test suite, 0 failures).
- Git commit created: `347bfb9 feat(network): add DTO models and F1ApiClient with unit tests`.

## Test Summary
5 unit test cases validating Ktor MockEngine request paths, query parameters, full JSON deserialization, and nullable safety passed with zero errors.

## Concerns
None. DTO contracts and client methods match the input specification for Task 5 (`RaceRepositoryImpl`), Task 10 (Live Timing), and Task 11 (Track Map).
