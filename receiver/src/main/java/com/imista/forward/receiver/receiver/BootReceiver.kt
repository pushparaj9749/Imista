package com.imista.forward.receiver.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.imista.forward.receiver.service.RelayPollingService

/** Re-arms relay polling after a reboot (legacy token polling is retired). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try { RelayPollingService.schedulePeriodic(context) } catch (_: Exception) {}
        }
    }
}
