# Task 7 Brief: Koin Dependency Injection Setup & Application Class

## Files to Create
- `app/src/main/java/com/f1racehub/app/di/AppModule.kt`
- `app/src/main/java/com/f1racehub/app/F1App.kt`
- `app/src/test/java/com/f1racehub/app/di/AppModuleTest.kt`

## Interfaces
- Consumes: Room, Ktor, `RaceDao`, `F1ApiClient`, `SessionAlarmScheduler`, `RaceRepositoryImpl`.
- Produces:
  - `val appModule = module { ... }`
  - `class F1App : Application() { ... }`
  - Unit test verifying Koin module declarations and resolution.

## Implementation Details

### 1. `app/src/main/java/com/f1racehub/app/di/AppModule.kt`
```kotlin
package com.f1racehub.app.di

import androidx.room.Room
import com.f1racehub.app.core.alarms.SessionAlarmScheduler
import com.f1racehub.app.data.local.F1Database
import com.f1racehub.app.data.remote.F1ApiClient
import com.f1racehub.app.data.repository.RaceRepositoryImpl
import com.f1racehub.app.domain.repository.RaceRepository
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    single {
        Room.databaseBuilder(
            androidContext(),
            F1Database::class.java,
            "f1_race_hub.db"
        ).fallbackToDestructiveMigration().build()
    }

    single { get<F1Database>().raceDao() }
    single { F1ApiClient(get()) }
    single { SessionAlarmScheduler(androidContext()) }
    single<RaceRepository> { RaceRepositoryImpl(get(), get()) }
}
```

### 2. `app/src/main/java/com/f1racehub/app/F1App.kt`
```kotlin
package com.f1racehub.app

import android.app.Application
import com.f1racehub.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class F1App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@F1App)
            modules(appModule)
        }
    }
}
```

### 3. Unit Test `app/src/test/java/com/f1racehub/app/di/AppModuleTest.kt`
Unit test validating that `appModule` definition declares the expected singletons (`HttpClient`, `F1Database`, `RaceDao`, `F1ApiClient`, `SessionAlarmScheduler`, `RaceRepository`).

### 4. Commit
Commit with message: `feat(di): configure Koin injection graph and F1App application entry`
