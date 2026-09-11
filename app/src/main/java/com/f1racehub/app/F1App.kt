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

            val sw = java.io.StringWriter()
            throwable.printStackTrace(java.io.PrintWriter(sw))
            val errorText = "Thread: ${thread.name}\n${throwable::class.java.name}: ${throwable.message}\n\nStack:\n$sw"

            try {
                val file = java.io.File(filesDir, "crash_log.txt")
                file.writeText(errorText)
            } catch (_: Throwable) {}

            try {
                val intent = android.content.Intent(this@F1App, CrashActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("error_details", errorText)
                }
                startActivity(intent)
            } catch (e: Throwable) {
                Log.e("F1RaceHub", "Failed to launch CrashActivity", e)
            }
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