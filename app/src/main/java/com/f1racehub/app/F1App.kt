package com.f1racehub.app

import android.app.Application
import android.util.Log
import com.f1racehub.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class F1App : Application() {

    companion object {
        @Volatile
        var startupError: Throwable? = null
    }

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("F1RaceHub", "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}", throwable)
            startupError = throwable
        }

        try {
            startKoin {
                androidLogger(Level.ERROR)
                androidContext(this@F1App)
                modules(appModule)
            }
        } catch (t: Throwable) {
            Log.e("F1RaceHub", "Error starting Koin", t)
            startupError = t
        }
    }
}