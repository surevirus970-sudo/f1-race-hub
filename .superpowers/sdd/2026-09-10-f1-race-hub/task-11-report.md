# Task 11 Report: Interactive Live Track Map (`TrackMapCanvas`)

## Status
DONE

## Implemented Files
1. `app/src/main/java/com/f1racehub/app/presentation/screens/trackmap/TrackMapViewModel.kt`
   - Экспортирует модели данных `CarTrackPosition` (`driverNumber: Int`, `driverCode: String`, `teamColorHex: String`, `normX: Float`, `normY: Float`) и `TrackMapUiState` (`circuitName: String = "Autodromo Nazionale Monza"`, `cars: List<CarTrackPosition> = emptyList()`, `isLoading: Boolean = false`).
   - Наследует `androidx.lifecycle.ViewModel` и инжектирует опциональные зависимости `F1ApiClient? = null`, `CoroutineDispatcher = Dispatchers.Default` и флаг `shouldSimulate: Boolean = true`.
   - Реализует чистую функцию нормализации `normalizeCoordinates(x: Double, y: Double, minX: Double, maxX: Double, minY: Double, maxY: Double): Pair<Float, Float>`, вычисляющую линейную интерполяцию с ограничением значений диапазоном `0.0f..1.0f` через `coerceIn` и защитой от деления на ноль при вырожденных границах `min >= max`.
   - Поддерживает ручное обновление телеметрии болидов через `updateCars(newCars: List<CarTrackPosition>)`.
   - Запускает корутину симуляции с интервалом 100 мс в `viewModelScope`, обновляющую орбитальные координаты болидов (VER #1, NOR #4, LEC #16, HAM #44) по тригонометрической траектории с шагом фазы 0.05 рад.

2. `app/src/main/java/com/f1racehub/app/presentation/screens/trackmap/TrackMapScreen.kt`
   - Главный компонент `@Composable fun TrackMapScreen(viewModel: TrackMapViewModel)` с подложкой `F1Background` (`#101014`).
   - Верхняя панель содержит заголовок "КАРТА ТРАССЫ", наименование автодрома `state.circuitName` и анимированный пульсирующий бейдж `LIVE` с циклом альфа-канала 800 мс на базе `rememberInfiniteTransition`.
   - Центральная область `Box` рендерит `Canvas` с размерами за вычетом отступа 36.dp:
     - Базовый асфальтовый контур трассы толщиной 16.dp (`Color(0xFF23232C)`).
     - Траектория гоночной линии толщиной 2.dp (`Color(0xFF3F3F4E)`).
     - Засечка линии старт/финиш на апексе верхнего сектора толщиной 3.dp.
     - Маркеры болидов: внешнее белое кольцо ореола радиусом 12.dp, внутренний диск цвета команды радиусом 10.dp и центральная точка радиусом 3.dp.
   - Нижний блок `Card` (`F1Surface`, обводка `F1SurfaceBorder`) выводит плашки пилотов на трассе с цветовыми индикаторами команд, кодами и номерами.

3. `app/src/main/java/com/f1racehub/app/di/AppModule.kt`
   - Добавлен модуль `presentationModule` с определением `viewModel { TrackMapViewModel(getOrNull()) }`.
   - Модуль включен в граф зависимостей через `includes(presentationModule)` в `appModule`, гарантируя сохранение 6 корневых синглтонов `appModule.mappings` без деградации контракта `AppModuleTest`.
   - Устранены star-импорты Ktor с заменой на точечные пакеты (`io.ktor.client.HttpClient`, `CIO`, `ContentNegotiation`, `json`).

4. `app/src/test/java/com/f1racehub/app/presentation/screens/trackmap/TrackMapViewModelTest.kt`
   - 6 модульных тестов на базе JUnit 5 и `kotlinx-coroutines-test`:
     - Проверка начального состояния: автодром `"Autodromo Nazionale Monza"`, флаг `isLoading == false`, 4 валидных болида с hex-цветами `#RRGGBB` и координатами в диапазоне `[0.0, 1.0]`.
     - Проверка `normalizeCoordinates`: интерполяция `(50.0, 75.0) -> (0.5, 0.75)`, отсечение отрицательных выходов за границу в `0.0f`, отсечение положительных выходов в `1.0f`, обработка вырожденного интервала `minX == maxX`.
     - Проверка `updateCars`: мгновенная синхронизация состояния `StateFlow`.
     - Проверка дискретного шага `advanceSimulationStep`: сохранение размера списка, детекция движения болидов и удержание координат в границах `[0.0, 1.0]`.
     - Проверка фонового тикера `startSimulation` и `stopSimulation`: корректное смещение координат за 250 мс и фиксация позиций после отмены задачи.
     - Проверка резолвинга Koin: фабричное инстанциирование `TrackMapViewModel` из `appModule`.

5. `.superpowers/sdd/2026-09-10-f1-race-hub/task-11-diff.patch`
   - Патч изменений коммита `6cd6344`.

## Verification
- Исходный код скомпилирован с использованием `kotlinc` 2.0.20 с плагинами Compose и Kotlinx Serialization под Android SDK 35 (API 35/37 platform stubs).
- Запущен полный тестовый набор проекта: 83 теста пройдено, 0 ошибок в 10 тестовых классах:
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
- Регрессия по 77 исходным тестам: 0.

## Commit
- `6cd6344 feat(ui): implement TrackMapScreen with Canvas track rendering and live car markers`
