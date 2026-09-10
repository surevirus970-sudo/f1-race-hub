# Task 11 Brief: Interactive Live Track Map (`TrackMapCanvas`)

## Goal
Implement the interactive live track map module displaying circuit layout and real-time normalized car positions on a high-performance Jetpack Compose `Canvas`.

## Scope of Work
1. **ViewModel & Data Classes** (`app/src/main/java/com/f1racehub/app/presentation/screens/trackmap/TrackMapViewModel.kt`):
   - `CarTrackPosition`:
     - `driverNumber: Int`
     - `driverCode: String`
     - `teamColorHex: String`
     - `normX: Float` (in range 0.0f..1.0f)
     - `normY: Float` (in range 0.0f..1.0f)
   - `TrackMapUiState`:
     - `circuitName: String = "Autodromo Nazionale Monza"`
     - `cars: List<CarTrackPosition> = emptyList()`
     - `isLoading: Boolean = false`
   - `TrackMapViewModel`:
     - Exposes `state: StateFlow<TrackMapUiState>`.
     - Normalization helper method `normalizeCoordinates(x: Double, y: Double, minX: Double, maxX: Double, minY: Double, maxY: Double): Pair<Float, Float>` ensuring output clamped to `0f..1f`.
     - `updateCars(newCars: List<CarTrackPosition>)` to allow state updates.
     - `simulateCarsOnTrack()` or simulation loop running in `viewModelScope` with a 100ms ticker, updating coordinates along an orbital path.
     - Optional constructor injection of `F1ApiClient? = null` and `CoroutineDispatcher = Dispatchers.Default` for deterministic unit testing.

2. **Compose Screen** (`app/src/main/java/com/f1racehub/app/presentation/screens/trackmap/TrackMapScreen.kt`):
   - Header with title "КАРТА ТРАССЫ", subtitle with circuit name, and live pulse status badge.
   - Main area with `Box` and `Canvas`:
     - Canvas draws track outline using `Stroke` (circuit path/geometry with dark gray track surface and racing line accents).
     - Canvas iterates over `state.cars` and draws car markers:
       - Outer halo / border circle (`Color.White`, radius ~12.dp.toPx()).
       - Inner team-colored circle (`Color(android.graphics.Color.parseColor(car.teamColorHex))`, radius ~10.dp.toPx()).
     - Bottom legend card displaying active cars on track with their team colors and driver codes.
   - Background `F1Background` (`#101014`), Material 3 styling.

3. **Unit Tests** (`app/src/test/java/com/f1racehub/app/presentation/screens/trackmap/TrackMapViewModelTest.kt`):
   - Using JUnit 5 (`org.junit.jupiter.api.Test`, `Assertions.*`) and Kotlin Coroutines Test (`StandardTestDispatcher` or `UnconfinedTestDispatcher`).
   - Test 1: Initial state emits correct circuit name and valid initial car list.
   - Test 2: `normalizeCoordinates` accurately maps raw telemetry to `[0.0, 1.0]` and clamps out-of-bound values.
   - Test 3: `updateCars` updates the state flow immediately.
   - Test 4: Simulation step advances car coordinates within the valid normalized range `0f..1f`.

4. **Koin Module Registration** (`app/src/main/java/com/f1racehub/app/di/AppModule.kt`):
   - Add `viewModel { TrackMapViewModel(getOrNull()) }` so `koinViewModel<TrackMapViewModel>()` resolves cleanly.

## Testing & Verification
- Run tests via `gradlew.bat testDebugUnitTest --tests "com.f1racehub.app.presentation.screens.trackmap.*"`
- Verify zero regression across existing 77 project unit tests.

## Commit Format
`feat(ui): implement TrackMapScreen with Canvas track rendering and live car markers`
