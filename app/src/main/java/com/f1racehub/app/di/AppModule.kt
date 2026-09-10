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