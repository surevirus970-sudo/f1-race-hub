# Архитектурная спецификация: Мобильное приложение F1 Race Hub для Android

- **Дата:** 2026-09-10
- **Статус:** Утверждено пользователем (Ready for implementation plan)
- **Целевая платформа:** Android (minSdk 26, targetSdk 35)
- **Основной технологический стек:** Kotlin 2.x, Jetpack Compose, Material 3, Room DB, Koin, Ktor Client, Coil 3.x

---

## 1. Назначение и функциональный объем (Scope)

Приложение предназначено для оперативного отслеживания гоночных уикендов Формулы-1, соревнований в реальном времени и сезонной статистики в автономном (offline-first) и сетевом режимах.

### 1.1. Ключевые возможности
1. **Дашборд этапа (Dashboard):** Таймер обратного отсчета до ближайшей сессии с точностью до секунды, расписание всех заездов уикенда (FP1, FP2, FP3, Qualifying, Sprint, Race) в локальном времени устройства, карточка характеристик автодрома.
2. **Живой тайминг (Live Timing Tower):** Башня позиций пилотов во время сессий, цветовая маркировка команд, интервалы до лидера и впереди идущего болида, составы резины Pirelli с числом пройденных кругов, статус гоночного контроля (флаги, Safety Car, Virtual Safety Car).
3. **Интерактивная карта трассы (Live Track Map):** Векторная отрисовка трека на `Canvas` с динамическим позиционированием болидов по декартовым координатам телеметрии и сглаживанием перемещения алгоритмом `lerp`.
4. **Турнирные таблицы (Standings):** Личный зачет пилотов с официальными фото-портретами и Кубок Конструкторов с визуализацией отрывов по очкам.
5. **Локальные напоминания (Race Alarms):** Гарантированное оповещение пользователя за 15 минут до старта выбранной сессии через `AlarmManager` без внешних push-серверов.

---

## 2. Архитектура системы и организация слоев (Clean Architecture)

Кодовая база разбита на четыре изолированных функциональных пакета:

```text
com.f1racehub.app/
├── core/
│   ├── network/            # Конфигурация Ktor Client, сериализация JSON, обработка ошибок
│   ├── database/           # Room Database, TypeConverters, миграции
│   ├── alarms/             # AlarmManager, BroadcastReceiver, NotificationManager
│   └── time/               # Утилиты ZonedDateTime, парсеры ISO-8601, таймер
├── data/
│   ├── remote/
│   │   ├── jolpica/        # DTO и клиенты API Jolpica-F1 (календарь, зачеты)
│   │   └── openf1/         # DTO и клиенты API OpenF1 (тайминг, телеметрия, координаты)
│   ├── local/
│   │   ├── entities/       # Сущности Room DB (races, sessions, standings)
│   │   └── dao/            # Data Access Objects (RaceDao, StandingsDao)
│   └── repository/         # Реализации интерфейсов репозиториев (Offline-first)
├── domain/
│   ├── model/              # Чистые Kotlin Data Classes без сторонних зависимостей
│   └── repository/         # Контракты репозиториев (RaceRepository, TimingRepository)
└── presentation/
    ├── navigation/         # NavHost, маршруты экранов, BottomNavigationBar
    ├── theme/              # F1Colors, F1Typography, F1Theme
    ├── components/         # Переиспользуемые Compose-компоненты (таймер, шины, флаги)
    └── screens/
        ├── dashboard/      # DashboardScreen, DashboardViewModel, DashboardUiState
        ├── timing/          # LiveTimingScreen, TimingViewModel, TimingUiState
        ├── trackmap/       # TrackMapScreen, TrackMapCanvas, TrackMapViewModel
        └── standings/      # StandingsScreen, StandingsViewModel, StandingsUiState
```

---

## 3. Модель данных и внешние контракты (API)

### 3.1. Внешние интеграции

#### Jolpica-F1 REST API (Базовые данные сезона)
* **Календарь этапов:**
  `GET https://api.jolpica.com/ergast/f1/current.json`
  Формат ответа: массив `MRData.RaceTable.Races`, содержащий названия этапов, дату/время каждой сессии в UTC, географические координаты и конфигурацию трека.
* **Личный зачет пилотов:**
  `GET https://api.jolpica.com/ergast/f1/current/driverStandings.json`
  Формат ответа: позиция, набранные очки, победы, идентификатор пилота, номер, трехбуквенный код, конструктор.
* **Кубок Конструкторов:**
  `GET https://api.jolpica.com/ergast/f1/current/constructorStandings.json`
  Формат ответа: позиция команды, очки, победы, идентификатор команды.

#### OpenF1 REST API (Оперативная телеметрия и Live-сессии)
* **Текущая/последняя сессия:**
  `GET https://api.openf1.org/v1/sessions?session_key=latest`
* **Интервалы и тайминг кругов:**
  `GET https://api.openf1.org/v1/intervals?session_key={session_key}`
  Поля: `driver_number`, `gap_to_leader`, `interval`.
* **Составы резины (Stints):**
  `GET https://api.openf1.org/v1/stints?session_key={session_key}`
  Поля: `driver_number`, `compound` (`SOFT`, `MEDIUM`, `HARD`, `INTERMEDIATE`, `WET`), `tyre_age_at_start`, `laps_new`.
* **Координаты болидов на трассе:**
  `GET https://api.openf1.org/v1/location?session_key={session_key}`
  Поля: `driver_number`, `date`, `x`, `y`, `z` (декартовы координаты болида в миллиметрах).
* **Гоночный контроль (Флаги):**
  `GET https://api.openf1.org/v1/race_control?session_key={session_key}`
  Поля: `flag` (`GREEN`, `YELLOW`, `RED`, `DOUBLE YELLOW`, `CHEQUERED`), `scope`, `message`.

### 3.2. Схема локальной базы данных (Room DB)

* **Таблица `races`:**
  * `round` (Int, Primary Key)
  * `raceName` (String)
  * `circuitId` (String)
  * `circuitName` (String)
  * `country` (String)
  * `locality` (String)
  * `date` (String, ISO-8601 UTC)
  * `isCompleted` (Boolean)
* **Таблица `race_sessions`:**
  * `sessionId` (String, Primary Key: `{round}_{sessionType}`)
  * `round` (Int, Foreign Key к `races.round` с `ON DELETE CASCADE`)
  * `sessionType` (String: `FP1`, `FP2`, `FP3`, `QUALIFYING`, `SPRINT`, `RACE`)
  * `startTimeUtc` (String, ISO-8601 UTC)
  * `isAlarmSet` (Boolean, дефолт `false`)
* **Таблица `driver_standings`:**
  * `driverId` (String, Primary Key)
  * `position` (Int)
  * `points` (Double)
  * `wins` (Int)
  * `driverCode` (String: `VER`, `HAM`, `NOR` и т.д.)
  * `permanentNumber` (String)
  * `givenName` (String)
  * `familyName` (String)
  * `constructorId` (String)
  * `constructorName` (String)
  * `headshotUrl` (String, nullable)
* **Таблица `constructor_standings`:**
  * `constructorId` (String, Primary Key)
  * `position` (Int)
  * `points` (Double)
  * `wins` (Int)
  * `name` (String)
  * `teamColorHex` (String)

---

## 4. Математическая модель отрисовки карты трека (`LiveTrackMap`)

1. **Нормализация координат телеметрии:**
   Для каждого автодрома вычисляется предельный диапазон координат:
   $$X_{\text{span}} = X_{\max} - X_{\min}, \quad Y_{\text{span}} = Y_{\max} - Y_{\min}$$
   
   Преобразование координат точки болида $(x_i, y_i)$ в координаты холста $(x_{\text{canvas}}, y_{\text{canvas}})$ при размерах $W \times H$:
   $$x_{\text{norm}} = \frac{x_i - X_{\min}}{X_{\text{span}}}, \quad y_{\text{norm}} = \frac{y_i - Y_{\min}}{Y_{\text{span}}}$$
   $$x_{\text{canvas}} = \text{padding} + x_{\text{norm}} \times (W - 2 \cdot \text{padding})$$
   $$y_{\text{canvas}} = H - \left(\text{padding} + y_{\text{norm}} \times (H - 2 \cdot \text{padding})\right)$$

2. **Интерполяция перемещения маркеров:**
   Для исключения дискретных скачков маркеров при частоте опроса API в 1–3 секунды применяется интерполяция между предыдущей позицией $P_0(x_0, y_0)$ и новой позицией $P_1(x_1, y_1)$ с параметром $t \in [0, 1]$:
   $$P(t) = P_0 + t \cdot (P_1 - P_0)$$
   В Compose это реализуется корутиной `Animatable.animateTo()` с кривой сплайна `FastOutSlowInEasing`.

---

## 5. Подсистема напоминаний и оповещений (Alarms & Notifications)

* **Манифест (`AndroidManifest.xml`):**
  * `android.permission.RECEIVE_BOOT_COMPLETED` (для восстановления будильников после перезагрузки устройства).
  * `android.permission.SCHEDULE_EXACT_ALARM` (для гарантированного запуска сессионного воркера).
  * `android.permission.POST_NOTIFICATIONS` (для Android 13+).
* **Компоненты:**
  * `SessionAlarmScheduler`: фасад над `AlarmManager`. Регистрирует намерение через `setExactAndAllowWhileIdle()`. Время триггера:
    $$T_{\text{alarm}} = T_{\text{session\_start}} - 15 \text{ минут}$$
  * `SessionAlarmReceiver : BroadcastReceiver`: перехватывает намерение, проверяет валидность и передает команду в `NotificationHelper`.
  * `NotificationHelper`: отправляет уведомление в канал `f1_reminders_channel` с флагом `NotificationCompat.PRIORITY_HIGH`.
  * `BootCompletedReceiver : BroadcastReceiver`: читает из Room сессии с `isAlarmSet = true` и временем в будущем, восстанавливая их в `AlarmManager`.

---

## 6. Дизайн-система и палитра (F1 Dark Racing UI)

* **Цветовая схема `F1Colors`:**
  * `Background`: `#101014`
  * `Surface`: `#1B1B22`
  * `SurfaceBorder`: `#2C2C38`
  * `PrimaryAccent`: `#E10600` (F1 Red)
  * `TextPrimary`: `#FFFFFF`
  * `TextSecondary`: `#9E9EA8`
* **Цвета команд (Hex):**
  * Red Bull Racing: `#3671C6`
  * Ferrari: `#E8002D`
  * McLaren: `#FF8000`
  * Mercedes: `#27F4D2`
  * Aston Martin: `#229971`
  * Alpine: `#FF87BC`
  * Williams: `#64C4FF`
  * RB (Visa Cash App RB): `#6692FF`
  * Kick Sauber: `#52E252`
  * Haas: `#B6BABD`
* **Цвета шин Pirelli (Hex):**
  * Soft: `#E8002D`
  * Medium: `#FFF500`
  * Hard: `#FFFFFF`
  * Intermediate: `#39B54A`
  * Wet: `#00A3E0`

---

## 7. Внедрение зависимостей (Koin 3.x / 4.x)

* `networkModule`: определение Ktor HTTP Client с сериализатором Kotlinx, логгером и таймаутами.
* `databaseModule`: конфигурация инстанса Room Database, предоставление `RaceDao`, `SessionDao`, `StandingsDao`.
* `repositoryModule`: привязка реализаций к интерфейсам `RaceRepository`, `TimingRepository`, `StandingsRepository`.
* `useCaseModule`: регистрация бизнес-логики (`GetNextGrandPrixUseCase`, `GetLiveTelemetryUseCase`, `ScheduleAlarmUseCase`).
* `viewModelModule`: фабрики `DashboardViewModel`, `LiveTimingViewModel`, `TrackMapViewModel`, `StandingsViewModel`.

---

## 8. Стратегия тестирования и критерии приемки

1. **Unit-тесты репозиториев и парсеров (JUnit 5 + MockK):**
   * Корректный маппинг ответов Jolpica JSON в доменные сущности `GrandPrix`.
   * Парсинг OpenF1 интервалов и обработка отсутствующих данных (null-safety для пилотов в боксах).
2. **Unit-тесты временных преобразований:**
   * Проверка сдвига часовых поясов из UTC в смещения `+03:00`, `+09:00`, `-05:00` с сохранением корректного дня недели.
   * Валидация вычисления оставшегося времени в таймере обратного отсчета.
3. **Unit-тесты планировщика напоминаний:**
   * Запрет постановки будильника на прошедшую дату.
   * Корректная генерация уникального `requestCode` для каждого типа сессии на основе ID этапа.
4. **Интеграционные тесты локальной БД (Room InMemory Database):**
   * Вставка этапа, каскадное сохранение сессий уикенда и их реактивное считывание через `Flow`.
