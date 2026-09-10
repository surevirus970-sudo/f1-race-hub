# Task 12 Brief: Standings Screen with Driver Portraits (Coil 3.x)

## Goal
Implement the Championship Standings screen (`StandingsScreen`) providing Driver and Constructor standings with high-performance asynchronous driver portrait rendering via Coil 3.x `AsyncImage` and interactive tab navigation (`TabRow`).

## Scope of Work
1. **ViewModel & Data Classes** (`app/src/main/java/com/f1racehub/app/presentation/screens/standings/StandingsViewModel.kt`):
   - `DriverStandingUiModel`:
     - `position: Int`
     - `name: String`
     - `code: String`
     - `team: String`
     - `points: Double`
     - `photoUrl: String`
   - `ConstructorStandingUiModel`:
     - `position: Int`
     - `teamName: String`
     - `points: Double`
     - `teamColorHex: String`
   - `StandingsUiState`:
     - `selectedTab: Int = 0` (0: Drivers / Пилоты, 1: Constructors / Команды)
     - `isLoading: Boolean = false`
     - `drivers: List<DriverStandingUiModel>` (prepopulated with top drivers: Verstappen, Norris, Leclerc, Piastri, Sainz, Hamilton)
     - `constructors: List<ConstructorStandingUiModel>` (prepopulated with top teams: McLaren, Red Bull Racing, Ferrari, Mercedes)
   - `StandingsViewModel`:
     - Exposes `state: StateFlow<StandingsUiState>`.
     - `selectTab(index: Int)`: updates `selectedTab`.
     - `updateDrivers(drivers: List<DriverStandingUiModel>)` and `updateConstructors(constructors: List<ConstructorStandingUiModel>)`.

2. **Compose Screen** (`app/src/main/java/com/f1racehub/app/presentation/screens/standings/StandingsScreen.kt`):
   - Background: `F1Background` (`#101014`).
   - Header title: "ТАБЛИЦЫ ЧЕМПИОНАТА", Material 3 typography with `F1RedPrimary` accent.
   - Material 3 `TabRow` (containerColor = `F1Surface`, contentColor = `F1RedPrimary`):
     - Tab 0: "ПИЛОТЫ"
     - Tab 1: "КОМАНДЫ"
   - Tab 0 (Drivers List):
     - `LazyColumn` with driver rows:
       - Position number (e.g. "1", "2") in `F1TextWhite`, bold.
       - Coil 3.x `AsyncImage`:
         - `model = driver.photoUrl`
         - `contentDescription = driver.name`
         - Circular clip (`CircleShape`), background `Color(0xFF23232C)`, size `44.dp`.
       - Driver full name and team name (`F1TextMuted`, 12.sp).
       - Points indicator with PTS suffix (e.g. "331 PTS") in `F1RedPrimary`, bold.
   - Tab 1 (Constructors List):
     - `LazyColumn` with team rows:
       - Position number.
       - Team color bar (width 4.dp, height 24.dp).
       - Team name.
       - Points indicator in `F1RedPrimary`, bold.

3. **Koin Module Registration** (`app/src/main/java/com/f1racehub/app/di/AppModule.kt`):
   - Add `viewModel { StandingsViewModel() }` in `presentationModule`.

4. **Unit Tests** (`app/src/test/java/com/f1racehub/app/presentation/screens/standings/StandingsViewModelTest.kt`):
   - Test 1: Initial state emits tab 0 (Drivers) with valid drivers and constructors lists.
   - Test 2: `selectTab` switches between 0 and 1 cleanly.
   - Test 3: Standings data integrity: all positions >= 1, points >= 0, driver codes and URLs non-empty.
   - Test 4: `updateDrivers` and `updateConstructors` update StateFlow reactively.
   - Test 5: Koin resolves `StandingsViewModel` from `appModule`.

## Testing & Verification
- Run tests: `./gradlew.bat testDebugUnitTest`
- Ensure all 83+ unit tests pass with zero regressions.

## Commit Format
`feat(ui): implement StandingsScreen with driver portraits via Coil and tab selector`
