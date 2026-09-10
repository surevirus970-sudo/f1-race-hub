# Task 4 Brief: Room Local Database & Offline Schema (TDD)

## Files to Create/Modify
- Modify: `gradle/libs.versions.toml` (ensure ksp version and plugin declared)
- Modify: `build.gradle.kts` & `app/build.gradle.kts` (apply ksp plugin and dependency `ksp(libs.room.compiler)`)
- Create: `app/src/main/java/com/f1racehub/app/data/local/entities/RaceEntities.kt`
- Create: `app/src/main/java/com/f1racehub/app/data/local/dao/F1Dao.kt`
- Create: `app/src/main/java/com/f1racehub/app/data/local/F1Database.kt`
- Create: `app/src/test/java/com/f1racehub/app/data/local/F1DaoTest.kt`

## Interfaces
- Consumes: Android Room DB 2.6+.
- Produces:
  - `RaceEntity(round: Int, raceName: String, circuitId: String, circuitName: String, country: String, locality: String, isCompleted: Boolean)`
  - `RaceSessionEntity(sessionId: String, round: Int, sessionType: String, startTimeUtc: String, isAlarmSet: Boolean)`
  - `RaceWithSessions(race: RaceEntity, sessions: List<RaceSessionEntity>)`
  - `RaceDao`:
    - `insertRaces(races: List<RaceEntity>)`
    - `insertSessions(sessions: List<RaceSessionEntity>)`
    - `observeRacesWithSessions(): Flow<List<RaceWithSessions>>`
    - `updateAlarmStatus(sessionId: String, isSet: Boolean)`
    - `getScheduledAlarms(): List<RaceSessionEntity>`
  - `F1Database: RoomDatabase`: abstract fun `raceDao(): RaceDao`

## Implementation Details

### 1. `RaceEntities.kt`
```kotlin
package com.f1racehub.app.data.local.entities

import androidx.room.*

@Entity(tableName = "races")
data class RaceEntity(
    @PrimaryKey val round: Int,
    val raceName: String,
    val circuitId: String,
    val circuitName: String,
    val country: String,
    val locality: String,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "race_sessions",
    foreignKeys = [
        ForeignKey(
            entity = RaceEntity::class,
            parentColumns = ["round"],
            childColumns = ["round"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("round")]
)
data class RaceSessionEntity(
    @PrimaryKey val sessionId: String, // e.g. "1_RACE"
    val round: Int,
    val sessionType: String, // FP1, FP2, FP3, QUALIFYING, SPRINT, RACE
    val startTimeUtc: String,
    val isAlarmSet: Boolean = false
)

data class RaceWithSessions(
    @Embedded val race: RaceEntity,
    @Relation(
        parentColumn = "round",
        entityColumn = "round"
    )
    val sessions: List<RaceSessionEntity>
)
```

### 2. `F1Dao.kt`
```kotlin
package com.f1racehub.app.data.local.dao

import androidx.room.*
import com.f1racehub.app.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaces(races: List<RaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<RaceSessionEntity>)

    @Transaction
    @Query("SELECT * FROM races ORDER BY round ASC")
    fun observeRacesWithSessions(): Flow<List<RaceWithSessions>>

    @Query("UPDATE race_sessions SET isAlarmSet = :isSet WHERE sessionId = :sessionId")
    suspend fun updateAlarmStatus(sessionId: String, isSet: Boolean)

    @Query("SELECT * FROM race_sessions WHERE isAlarmSet = 1")
    suspend fun getScheduledAlarms(): List<RaceSessionEntity>
}
```

### 3. `F1Database.kt`
```kotlin
package com.f1racehub.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.f1racehub.app.data.local.dao.RaceDao
import com.f1racehub.app.data.local.entities.*

@Database(
    entities = [RaceEntity::class, RaceSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class F1Database : RoomDatabase() {
    abstract fun raceDao(): RaceDao
}
```

### 4. Unit Test `F1DaoTest.kt`
Write unit test verifying:
- Entity and relation mapping (`RaceWithSessions`).
- DAO insert and queries.
- Alarm status update.

### 5. Commit
Commit with message: `feat(database): implement Room database schema, entities and RaceDao`
