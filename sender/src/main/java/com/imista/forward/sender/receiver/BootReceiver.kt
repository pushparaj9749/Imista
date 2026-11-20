package com.imista.forward.sender.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.imista.forward.sender.service.ForwardingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-enqueue pending forwards and restart the status heartbeat.
            try { ForwardingService.enqueueAllPending(context) } catch (_: Exception) {}
            try {
                if (com.imista.forward.sender.security.SecureStorage(context).forwardingActive) {
                    com.imista.forward.sender.service.DeviceStatusWorker.schedulePeriodic(context)
                    com.imista.forward.sender.service.DeviceStatusWorker.enqueueNow(context)
                }
            } catch (_: Exception) {}
        }
    }
}
