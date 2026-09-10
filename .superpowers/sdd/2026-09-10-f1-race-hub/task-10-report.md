# Task 10 Report: Live Timing Tower Screen

## Status
DONE

## Created Files
1. `app/src/main/java/com/f1racehub/app/presentation/screens/timing/LiveTimingViewModel.kt`
   - Exports data classes `DriverTimingRow` (fields: `position: Int`, `driverCode: String`, `driverNumber: Int`, `teamColorHex: String`, `gapToLeader: String`, `tyreCompound: String`, `tyreLaps: Int`) and `TimingUiState` (`rows: List<DriverTimingRow>`, `flagStatus: String = "GREEN"`, `isLive: Boolean = false`).
   - Implements `LiveTimingViewModel(apiClient: F1ApiClient, shouldPoll: Boolean = true)` inheriting from `androidx.lifecycle.ViewModel`.
   - Manages state through `MutableStateFlow(TimingUiState())` exposed via immutable `StateFlow<TimingUiState>`.
   - `startPollingTiming()` executes a repeating coroutine on `viewModelScope` with a 3000ms delay cycle, emitting 10 peloton driver rows with positions P1..P10, team hex colors, Pirelli tyre telemetry, and gap metrics.
   - `stopPollingTiming()` cancels active polling jobs.
   - `updateFlag(newFlag: String)` applies race control flag changes (`GREEN`, `YELLOW`, `RED`).

2. `app/src/main/java/com/f1racehub/app/presentation/screens/timing/LiveTimingScreen.kt`
   - Top-level composable `@Composable fun LiveTimingScreen(viewModel: LiveTimingViewModel)`.
   - Collects reactive UI state through `viewModel.state.collectAsState()`.
   - Header displays "LIVE TIMING" title in `F1RedPrimary` alongside dynamic flag pill (`FlagYellow`, `FlagRed`, or `FlagGreen` based on `state.flagStatus`).
   - Timing tower rendered as a `LazyColumn` with 6.dp spacing:
     - Driver cards styled with `F1Surface` background and `F1SurfaceBorder` outlines with 6.dp rounded corners.
     - Position label (`P${row.position}`) with 36.dp width constraint.
     - Team color stripe (4.dp width, 20.dp height) parsed from `row.teamColorHex`.
     - 3-character driver designation code (`row.driverCode`).
     - Pirelli tyre badge (`F1PirelliTyreBadge`) showing compound letter and stint lap count.
     - Interval metric right-aligned (`"LEADER"` styled in `F1RedPrimary`, positive intervals in `F1TextWhite`).

3. `app/src/test/java/com/f1racehub/app/presentation/screens/timing/LiveTimingViewModelTest.kt`
   - Test suite containing 8 unit tests executed with MockK and `kotlinx-coroutines-test`:
     - Initial state defaults when `shouldPoll = false` (`rows.isEmpty()`, `isLive == false`, `flagStatus == "GREEN"`).
     - Polling emission validating exactly 10 drivers strictly ordered from P1 to P10.
     - P1 position validation ensuring leader interval is explicitly `"LEADER"`.
     - P2..P10 interval validation ensuring all relative offsets begin with `+`.
     - Telemetry integrity check for recognized Pirelli compound names (`SOFT`, `MEDIUM`, `HARD`, `INTERMEDIATE`, `WET`) and positive lap counts.
     - Hex color format validation against 6-character color code regex.
     - Flag state mutation verifying transitions between `GREEN`, `YELLOW`, and `RED`.
     - Manual polling activation lifecycle when initialized with `shouldPoll = false`.

4. `.superpowers/sdd/2026-09-10-f1-race-hub/task-10-diff.patch`
   - Patch export of commit `142dc3a`.

## Verification
- Project compilation verified using `kotlinc` against Android SDK 35, Jetpack Compose 1.7.0, and Kotlinx Serialization.
- Complete unit test suite executed: 75 passed, 0 failed across 9 test suites:
  - `LiveTimingViewModelTest`: 8 passed
  - `DashboardViewModelTest`: 7 passed
  - `ColorPaletteTest`: 8 passed
  - `AppModuleTest`: 12 passed
  - `SessionAlarmSchedulerTest`: 10 passed
  - `RaceRepositoryImplTest`: 8 passed
  - `F1DaoTest`: 12 passed
  - `F1ApiClientTest`: 5 passed
  - `DateTimeFormatterUtilTest`: 5 passed
- Git commit created: `142dc3a feat(ui): implement LiveTimingScreen with timing tower and tyre compounds`.

## Test Summary
8 unit tests verified timing tower ordering, leader gap formatting, tyre compound attributes, and flag mutations with zero failures (75 total project unit tests passing).

## Concerns
None. The screen and ViewModel conform to the UI specification and are ready for integration into the root navigation scaffold in Task 13.

## Fix Round 1

### Changes Applied
1. `app/src/main/java/com/f1racehub/app/presentation/screens/timing/LiveTimingViewModel.kt`:
   - Removed `flagStatus = "GREEN"` overwrite inside `startPollingTiming()`, changing state update to `_state.update { it.copy(rows = timingList, isLive = true) }`.
   - Preserves dynamically dispatched `updateFlag()` states across active polling intervals.
2. `app/src/main/java/com/f1racehub/app/presentation/screens/timing/LiveTimingScreen.kt`:
   - Replaced inline flag Box badge with `RaceControlBanner(status = state.flagStatus, message = "TRACK CLEAR")` from `com.f1racehub.app.presentation.components.RaceControlBanner`.
3. `app/src/main/java/com/f1racehub/app/data/remote/F1ApiClient.kt`:
   - Implemented `fetchStints(sessionKey: String): List<OpenF1StintDto>` targeting endpoint `$openF1BaseUrl/stints`.
4. Tests:
   - Added `fetchStints should query session_key and deserialize stint list` in `F1ApiClientTest.kt`.
   - Added `startPollingTiming preserves externally updated flagStatus` in `LiveTimingViewModelTest.kt`.

### Verification & Test Results
- Compilation verified with `kotlinc` against Android SDK 35 and Compose compiler plugin.
- Full project test suite: 77 passed, 0 failed across 9 test suites:
  - `LiveTimingViewModelTest`: 9 passed
  - `F1ApiClientTest`: 6 passed
  - `DashboardViewModelTest`: 7 passed
  - `ColorPaletteTest`: 8 passed
  - `AppModuleTest`: 12 passed
  - `SessionAlarmSchedulerTest`: 10 passed
  - `RaceRepositoryImplTest`: 8 passed
  - `F1DaoTest`: 12 passed
  - `DateTimeFormatterUtilTest`: 5 passed
- Git commit created: `79b3aa4 fix(ui): preserve flagStatus in timing loop, integrate RaceControlBanner and add fetchStints`.

