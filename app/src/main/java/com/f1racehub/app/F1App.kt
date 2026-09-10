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