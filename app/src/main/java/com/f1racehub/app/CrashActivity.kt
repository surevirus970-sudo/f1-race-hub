package com.f1racehub.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val details = intent.getStringExtra("error_details") ?: "No details provided"

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            setPadding(40, 60, 40, 60)
        }
        val textView = TextView(this).apply {
            setTextColor(0xFFFF4444.toInt())
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            text = "=== F1 RACE HUB CRASH REPORT ===\n\n" + details
        }
        scrollView.addView(textView)
        setContentView(scrollView)
    }
}
