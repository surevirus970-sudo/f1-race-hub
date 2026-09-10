# Task 13 Brief: Main Scaffold, Bottom Navigation & App Integration

## Goal
Implement the core application entry point (`MainActivity.kt`) with Material 3 `Scaffold`, unified `NavigationBar` (BottomNavigation), and Jetpack Compose Navigation (`NavHost`), integrating all 4 application screens (`DashboardScreen`, `LiveTimingScreen`, `TrackMapScreen`, `StandingsScreen`) with Koin ViewModels. Also incorporate deferred items from Task 9.

## Scope of Work
1. **Navigation Routes** (`app/src/main/java/com/f1racehub/app/presentation/navigation/Screen.kt`):
   - `sealed class Screen(val route: String, val title: String, val icon: ImageVector)`:
     - `Dashboard`: route "dashboard", title "Этап", icon `Icons.Filled.Home`
     - `Timing`: route "timing", title "Тайминг", icon `Icons.Filled.List`
     - `TrackMap`: route "trackmap", title "Трек", icon `Icons.Filled.Place`
     - `Standings`: route "standings", title "Зачет", icon `Icons.Filled.Star`

2. **Main Activity** (`app/src/main/java/com/f1racehub/app/MainActivity.kt`):
   - Single-Activity architecture extending `ComponentActivity`.
   - `onCreate`: sets content wrapped in `F1Theme`.
   - Material 3 `Scaffold` with `bottomBar = { NavigationBar(containerColor = F1Surface) { ... } }`:
     - 4 tabs with `NavigationBarItem`:
       - `selectedIconColor = F1RedPrimary`
       - `selectedTextColor = F1RedPrimary`
       - `indicatorColor = F1Background`
       - `unselectedIconColor = F1TextMuted`
       - `unselectedTextColor = F1TextMuted`
       - Click handler navigating with `popUpTo(findStartDestination().id) { saveState = true }`, `launchSingleTop = true`, `restoreState = true`.
   - `NavHost` with `startDestination = Screen.Dashboard.route`:
     - `composable(Screen.Dashboard.route)`: injects `koinViewModel<DashboardViewModel>()` and renders `DashboardScreen(vm)`.
     - `composable(Screen.Timing.route)`: injects `koinViewModel<LiveTimingViewModel>()` and renders `LiveTimingScreen(vm)`.
     - `composable(Screen.TrackMap.route)`: injects `koinViewModel<TrackMapViewModel>()` and renders `TrackMapScreen(vm)`.
     - `composable(Screen.Standings.route)`: injects `koinViewModel<StandingsViewModel>()` and renders `StandingsScreen(vm)`.

3. **Incorporate Task 9 Deferred Items**:
   - In `app/src/main/java/com/f1racehub/app/presentation/screens/dashboard/DashboardScreen.kt`:
     - Add `modifier = Modifier.weight(1f)` to `LazyColumn` for session timeline.
     - Dynamic upcoming session calculation: select first session with `startTime.toInstant().isAfter(Instant.now())` or fallback to first session.
   - In `app/src/main/java/com/f1racehub/app/di/AppModule.kt`:
     - Register `DashboardViewModel` and `LiveTimingViewModel` in `presentationModule`.

4. **Unit Tests** (`app/src/test/java/com/f1racehub/app/presentation/navigation/NavigationTest.kt`):
   - Verify Screen definitions, routes, titles and icons.
   - Verify Koin resolution for all 4 ViewModels from `appModule`.

## Testing & Verification
- Run `cmd.exe /c "gradlew.bat testDebugUnitTest"`
- Verify 90+ unit tests pass with zero failures.

## Commit Format
`feat(app): integrate MainActivity with Scaffold, BottomNavigation and NavHost`
