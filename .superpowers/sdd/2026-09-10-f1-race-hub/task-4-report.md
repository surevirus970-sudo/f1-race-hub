# Task 4 Report: Room Local Database & Offline Schema (TDD)

## Status
DONE

## Created and Modified Files
1. `app/src/main/java/com/f1racehub/app/data/local/entities/RaceEntities.kt`
   - `RaceEntity`: SQLite table `races` with primary key `round: Int`, circuit metadata fields (`circuitId`, `circuitName`, `country`, `locality`), and completion flag `isCompleted: Boolean` (default `false`).
   - `RaceSessionEntity`: SQLite table `race_sessions` with primary key `sessionId: String`, foreign key referencing `races(round)` with `onDelete = ForeignKey.CASCADE`, index on `round`, session category `sessionType: String`, UTC ISO-8601 start timestamp `startTimeUtc: String`, and notification flag `isAlarmSet: Boolean` (default `false`).
   - `RaceWithSessions`: 1-to-many relational container embedding `RaceEntity` and binding `sessions: List<RaceSessionEntity>` via `@Relation(parentColumn = "round", entityColumn = "round")`.
2. `app/src/main/java/com/f1racehub/app/data/local/dao/F1Dao.kt`
   - `RaceDao` interface declaring Room persistence methods.
   - `insertRaces(races: List<RaceEntity>)`: batch insertion with `OnConflictStrategy.REPLACE`.
   - `insertSessions(sessions: List<RaceSessionEntity>)`: batch insertion with `OnConflictStrategy.REPLACE`.
   - `observeRacesWithSessions()`: `@Transaction` query `SELECT * FROM races ORDER BY round ASC` returning reactive `Flow<List<RaceWithSessions>>`.
   - `updateAlarmStatus(sessionId: String, isSet: Boolean)`: atomic query updating `isAlarmSet` column for targeted session identifier.
   - `getScheduledAlarms()`: query `SELECT * FROM race_sessions WHERE isAlarmSet = 1` retrieving active alarm configurations.
3. `app/src/main/java/com/f1racehub/app/data/local/F1Database.kt`
   - `F1Database: RoomDatabase`: abstract database definition binding entities `[RaceEntity::class, RaceSessionEntity::class]`, schema version `1`, `exportSchema = false`, and abstract accessor `raceDao(): RaceDao`.
4. `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`
   - Added KSP version `2.0.20-1.0.25` aligned with Kotlin `2.0.20`.
   - Declared plugin `com.google.devtools.ksp` in version catalog and applied to root and module Gradle scripts.
   - Added `ksp(libs.room.compiler)` dependency in `app/build.gradle.kts`.
5. `app/src/test/java/com/f1racehub/app/data/local/F1DaoTest.kt`
   - Unit test suite verifying:
     - Ordered extraction and 1-to-many parent-child correlation in `observeRacesWithSessions`.
     - `OnConflictStrategy.REPLACE` semantics on repeated race insertions.
     - Primary key conflict resolution on session insertions.
     - State transitions in `updateAlarmStatus` (enabling and disabling alarm flags).
     - Filtering accuracy of `getScheduledAlarms` against sessions with `isAlarmSet = 0` and `isAlarmSet = 1`.
     - Reactive emissions through `MutableStateFlow` updates upon session insertion.
     - Model encapsulation in `RaceWithSessions`.
     - Default parameter assignments across data class constructors.
     - Bytecode-level inspection validating `RetentionPolicy.CLASS` Room annotations on `F1Database`, `RaceDao`, `RaceEntity`, `RaceSessionEntity`, and `RaceWithSessions`.

## Verification
- Compilation verified with `kotlinc-jvm 2.3.10` targeting JRE 25 against Android 35 platform classes and Room 2.6.1 runtime libraries.
- Full test suite executed comprising 22 passing tests (12 new tests in `F1DaoTest`, 5 tests in `F1ApiClientTest`, 5 tests in `DateTimeFormatterUtilTest`) with zero errors.
- Git commit created: `b5be09f feat(database): implement Room database schema, entities and RaceDao`.

## Test Summary
12 unit test cases verifying Room entities, 1-to-many relations, DAO queries, reactive updates, alarm flag persistence, and class-level Room annotations passed with zero failures.

## Concerns
None. Schema contracts and DAO methods match all consuming specifications for Task 5 (`RaceRepositoryImpl`) and Task 6 (`SessionAlarmScheduler`).
