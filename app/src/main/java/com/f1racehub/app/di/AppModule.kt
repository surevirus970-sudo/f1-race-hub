package com.f1racehub.app.di

import androidx.room.Room
import com.f1racehub.app.core.alarms.SessionAlarmScheduler
import com.f1racehub.app.data.local.F1Database
import com.f1racehub.app.data.remote.F1ApiClient
import com.f1racehub.app.data.repository.RaceRepositoryImpl
import com.f1racehub.app.domain.repository.RaceRepository
import com.f1racehub.app.presentation.screens.standings.StandingsViewModel
import com.f1racehub.app.presentation.screens.trackmap.TrackMapViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { TrackMapViewModel(getOrNull()) }
    viewModel { StandingsViewModel() }
}

val appModule = module {
    includes(presentationModule)

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