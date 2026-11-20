package com.imista.forward.receiver.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.imista.forward.receiver.service.PollingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try { PollingService.schedulePeriodic(context) } catch (_: Exception) {}
        }
    }
}
