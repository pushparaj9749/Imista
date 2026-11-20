package com.imista.forward.sender

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class ImistaSenderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize WorkManager with custom config if needed
        try {
            WorkManager.getInstance(this)
        } catch (e: Exception) {
            // WorkManager not initialized, will init via default
        }
    }
}
