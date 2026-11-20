package com.imista.forward.sender.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class ImistaDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        context.getSharedPreferences("imista_status", Context.MODE_PRIVATE).edit()
            .putBoolean("device_admin_enabled", true).apply()
        Toast.makeText(context, "Imista Device Admin enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        context.getSharedPreferences("imista_status", Context.MODE_PRIVATE).edit()
            .putBoolean("device_admin_enabled", false).apply()
        Toast.makeText(context, "Imista Device Admin disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling Device Admin will allow uninstalling Imista Forward Sender. Continue?"
    }
}
