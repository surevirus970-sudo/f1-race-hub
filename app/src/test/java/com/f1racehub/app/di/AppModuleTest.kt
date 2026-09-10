package com.f1racehub.app.di

import android.app.Application
import android.content.Context
import com.f1racehub.app.F1App
import com.f1racehub.app.core.alarms.SessionAlarmScheduler
import com.f1racehub.app.data.local.F1Database
import com.f1racehub.app.data.local.dao.RaceDao
import com.f1racehub.app.data.remote.F1ApiClient
import com.f1racehub.app.data.repository.RaceRepositoryImpl
import com.f1racehub.app.domain.repository.RaceRepository
import io.ktor.client.HttpClient
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.definition.Kind
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class AppModuleTest {

    @OptIn(KoinInternalApi::class)
    @Test
    fun appModule_declaresExactExpectedSingletonCount() {
        val mappings = appModule.mappings
        assertEquals(6, mappings.size, "appModule must register exactly 6 dependencies")
    }

    @OptIn(KoinInternalApi::class)
    @Test
    fun appModule_allDefinitionsHaveSingletonKind() {
        val definitions = appModule.mappings.values.map { it.beanDefinition }
        assertTrue(definitions.all { it.kind == Kind.Singleton }, "All definitions in appModule must have Kind.Singleton")
    }

    @OptIn(KoinInternalApi::class)
    @Test
    fun appModule_declaresHttpClientDefinition() {
        val hasHttpClient = appModule.mappings.values.any { 
            it.beanDefinition.hasType(HttpClient::class) 
        }
        assertTrue(hasHttpClient, "appModule must declare a binding for HttpClient")
    }

    @OptIn(KoinInternalApi::class)
    @Test
    fun appModule_declaresF1DatabaseDefinition() {
        val hasDatabase = appModule.mappings.values.any { 
            it.beanDefinition.hasType(F1Database::class) 
        }
        assertTrue(hasDatabase, "appModule must declare a binding for F1Database")
    }

    @OptIn(KoinInternalApi::class)
    @Test
    fun appModule_declaresRaceDaoDefinition() {
        val hasRaceDao = appModule.mappings.values.any { 
            it.beanDefinition.hasType(RaceDao::class) 
        }
        assertTrue(hasRaceDao, "appModule must declare a binding for RaceDao")
    }

    @OptIn(KoinInternalApi::class)
    @Test
    fun appModule_declaresF1ApiClientDefinition() {
        val hasApiClient = appModule.mappings.values.any { 
            it.beanDefinition.hasType(F1ApiClient::class) 
        }
        assertTrue(hasApiClient, "appModule must declare a binding for F1ApiClient")
    }

    @OptIn(KoinInternalApi::class)
    @Test
    fun appModule_declaresSessionAlarmSchedulerDefinition() {
        val hasAlarmScheduler = appModule.mappings.values.any { 
            it.beanDefinition.hasType(SessionAlarmScheduler::class) 
        }
        assertTrue(hasAlarmScheduler, "appModule must declare a binding for SessionAlarmScheduler")
    }

    @OptIn(KoinInternalApi::class)
    @Test
    fun appModule_declaresRaceRepositoryDefinition() {
        val hasRaceRepository = appModule.mappings.values.any { 
            it.beanDefinition.hasType(RaceRepository::class) 
        }
        assertTrue(hasRaceRepository, "appModule must declare a binding for RaceRepository")
    }

    @Test
    fun appModule_resolvesGraphInstancesWithIsolatedKoinApplication() {
        val mockContext = mockk<Context>(relaxed = true)
        val mockDatabase = mockk<F1Database>()
        val mockDao = mockk<RaceDao>()
        every { mockDatabase.raceDao() } returns mockDao

        val koinApp = koinApplication {
            androidContext(mockContext)
            allowOverride(true)
            modules(appModule, module {
                single<F1Database> { mockDatabase }
            })
        }

        try {
            val httpClient = koinApp.koin.get<HttpClient>()
            assertNotNull(httpClient, "HttpClient should resolve from container")

            val f1ApiClient = koinApp.koin.get<F1ApiClient>()
            assertNotNull(f1ApiClient, "F1ApiClient should resolve from container")

            val sessionAlarmScheduler = koinApp.koin.get<SessionAlarmScheduler>()
            assertNotNull(sessionAlarmScheduler, "SessionAlarmScheduler should resolve from container")

            val raceDao = koinApp.koin.get<RaceDao>()
            assertNotNull(raceDao, "RaceDao should resolve via F1Database delegation")
            assertSame(mockDao, raceDao, "Resolved RaceDao should match database DAO mock")

            val raceRepository = koinApp.koin.get<RaceRepository>()
            assertNotNull(raceRepository, "RaceRepository should resolve from container")
            assertTrue(raceRepository is RaceRepositoryImpl, "RaceRepository should bind to RaceRepositoryImpl")

            httpClient.close()
        } finally {
            koinApp.close()
        }
    }

    @Test
    fun appModule_singletonScopeCachesSameInstanceReferences() {
        val mockContext = mockk<Context>(relaxed = true)
        val mockDatabase = mockk<F1Database>()
        val mockDao = mockk<RaceDao>()
        every { mockDatabase.raceDao() } returns mockDao

        val koinApp = koinApplication {
            androidContext(mockContext)
            allowOverride(true)
            modules(appModule, module {
                single<F1Database> { mockDatabase }
            })
        }

        try {
            val repoFirst = koinApp.koin.get<RaceRepository>()
            val repoSecond = koinApp.koin.get<RaceRepository>()
            assertSame(repoFirst, repoSecond, "Multiple resolves of RaceRepository must return identical instance")

            val clientFirst = koinApp.koin.get<F1ApiClient>()
            val clientSecond = koinApp.koin.get<F1ApiClient>()
            assertSame(clientFirst, clientSecond, "Multiple resolves of F1ApiClient must return identical instance")

            val schedulerFirst = koinApp.koin.get<SessionAlarmScheduler>()
            val schedulerSecond = koinApp.koin.get<SessionAlarmScheduler>()
            assertSame(schedulerFirst, schedulerSecond, "Multiple resolves of SessionAlarmScheduler must return identical instance")

            val httpClient = koinApp.koin.get<HttpClient>()
            httpClient.close()
        } finally {
            koinApp.close()
        }
    }

    @Test
    fun f1App_inheritsFromAndroidApplication() {
        assertTrue(
            Application::class.java.isAssignableFrom(F1App::class.java),
            "F1App must extend android.app.Application"
        )
    }

    @Test
    fun f1App_declaresOnCreateLifecycleMethod() {
        val method = F1App::class.java.getDeclaredMethod("onCreate")
        assertNotNull(method, "F1App must declare onCreate lifecycle method")
    }
}