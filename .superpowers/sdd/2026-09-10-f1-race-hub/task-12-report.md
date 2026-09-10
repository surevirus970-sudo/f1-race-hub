# Task 12 Report: Standings Screen with Driver Portraits (Coil 3.x)

## Status
DONE

## Implemented Files
1. `app/src/main/java/com/f1racehub/app/presentation/screens/standings/StandingsViewModel.kt`
   - Экспортирует модели данных `DriverStandingUiModel` (`position: Int`, `name: String`, `code: String`, `team: String`, `points: Double`, `photoUrl: String`) и `ConstructorStandingUiModel` (`position: Int`, `teamName: String`, `points: Double`, `teamColorHex: String`).
   - Содержит состояние экрана `StandingsUiState` с переключателем `selectedTab: Int` (0 — зачет пилотов, 1 — Кубок конструкторов) и списками топ-пилотов сезона (Verstappen, Norris, Leclerc, Piastri, Sainz, Hamilton) и команд (McLaren, Red Bull Racing, Ferrari, Mercedes).
   - Предоставляет методы `selectTab(index: Int)`, `updateDrivers(...)` и `updateConstructors(...)` для реактивной мутации `StateFlow`.

2. `app/src/main/java/com/f1racehub/app/presentation/screens/standings/StandingsScreen.kt`
   - Главный composable `StandingsScreen(viewModel: StandingsViewModel)` в палитре `F1Background` (`#101014`).
   - Навигационный компонент `TabRow` с вкладками "ПИЛОТЫ" и "КОМАНДЫ" в контейнере `F1Surface` (`#1B1B22`) с активным акцентом `F1RedPrimary` (`#E10600`).
   - Список личного зачета `DriversStandingsList`:
     - Отрисовывает позицию пилота, имя, команду и сумму очков.
     - Интегрирует асинхронную загрузку аватаров пилотов через Coil 3.x `coil3.compose.AsyncImage` с круговой маской `CircleShape` и подложкой `Color(0xFF23232C)`.
   - Список командного зачета `ConstructorsStandingsList`:
     - Отрисовывает плашки команд с индикатором фирменного цвета команды (`teamColorHex`), наименованием конструктора и очками.

3. `app/src/main/java/com/f1racehub/app/di/AppModule.kt`
   - В модуль `presentationModule` добавлена регистрация `viewModel { StandingsViewModel() }`.

4. `app/src/test/java/com/f1racehub/app/presentation/screens/standings/StandingsViewModelTest.kt`
   - 5 модульных тестов на базе JUnit 5:
     - Проверка начального состояния: выбран таб 0, 6 пилотов, 4 конструктора.
     - Переключение табов 0 и 1 через `selectTab`.
     - Валидация инвариантов данных: неотрицательные очки, валидные позиции >= 1, непустые URL-адреса фотографий с протоколом HTTPS, hex-формат `#RRGGBB` цветов команд.
     - Реактивное обновление списков через `updateDrivers` и `updateConstructors`.
     - Разрешение зависимости `StandingsViewModel` через Koin-контейнер `appModule`.

## Verification & Test Results
- Полный прогон тестового набора проекта: 88 тестов в 11 тестовых классах завершены без ошибок (0 failures):
  - `StandingsViewModelTest`: 5 passed
  - `TrackMapViewModelTest`: 6 passed
  - `LiveTimingViewModelTest`: 9 passed
  - `DashboardViewModelTest`: 7 passed
  - `ColorPaletteTest`: 8 passed
  - `AppModuleTest`: 12 passed
  - `SessionAlarmSchedulerTest`: 10 passed
  - `RaceRepositoryImplTest`: 8 passed
  - `F1DaoTest`: 12 passed
  - `F1ApiClientTest`: 6 passed
  - `DateTimeFormatterUtilTest`: 5 passed
- Регрессия по 83 предшествующим тестам: 0.
