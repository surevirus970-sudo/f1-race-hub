package com.f1racehub.app.core.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    companion object {
        fun handleBootCompleted(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                // Регистрация будет переинициализирована при старте приложения через Room DAO
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        handleBootCompleted(context, intent)
    }
}
