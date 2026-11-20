package com.imista.forward.sender.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imista.forward.sender.security.SecureStorage
import com.imista.forward.sender.service.DeviceStatusWorker
import com.imista.forward.sender.viewmodel.SetupViewModel

/**
 * Hidden settings (open by tapping the IMISTA logo 6 times on the black screen).
 * Shows the phone's own live status and the few switches that matter.
 */
@Composable
fun MinimalSettingsScreen(
    vm: SetupViewModel = viewModel(),
    onBack: () -> Unit,
    onReset: () -> Unit,
    onOpenConnection: () -> Unit = {}
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val secure = remember { SecureStorage(ctx) }
    val status = remember { DeviceStatusWorker.collectStatus(ctx, secure) }
    val powerSaver = (ctx.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true

    Column(Modifier.fillMaxSize().background(Color(0xFF0B0B14)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Sender status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text("Forwarding phone ka live status. Ye screen chhupi hui hai — black screen pe logo ko 6 baar tap karke khulti hai.", color = Color(0xFF94A3B8), fontSize = 11.sp)

        Spacer(Modifier.height(14.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Phone", fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(10.dp))
                StatusRow("Battery", if ((status.batteryPct ?: -1) >= 0) "${status.batteryPct}%" else "—",
                    if ((status.batteryPct ?: 100) <= 15) Color(0xFFEF4444) else Color.White)
                StatusRow("Charging", if (status.charging == true) "Yes" else "No",
                    if (status.charging == true) Color(0xFF10B981) else Color(0xFF94A3B8))
                StatusRow("Network", status.network ?: "—", Color(0xFF38BDF8))
                StatusRow("Screen", if (status.screenOn == true) "On" else "Off", Color(0xFF94A3B8))
                StatusRow("Model", status.deviceModel ?: "—", Color(0xFF94A3B8))
                StatusRow("Android", status.appVersion ?: "—", Color(0xFF94A3B8))
                StatusRow("Free storage", "${status.storageFreeMb ?: 0} MB", Color(0xFF94A3B8))
                StatusRow("Uptime", "${status.uptimeMinutes ?: 0} min", Color(0xFF94A3B8))
                StatusRow("Battery saver", if (powerSaver) "ON (delays possible)" else "Off",
                    if (powerSaver) Color(0xFFF59E0B) else Color(0xFF94A3B8))
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Forwarding", fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(10.dp))
                StatusRow("State", if (state.forwardingActive) "Active" else "Stopped", if (state.forwardingActive) Color(0xFF10B981) else Color(0xFFEF4444))
                StatusRow("Notification access", if (state.notificationEnabled) "Granted" else "Missing", if (state.notificationEnabled) Color(0xFF10B981) else Color(0xFFEF4444))
                StatusRow("Total forwarded", secure.totalForwarded.toString(), Color(0xFF38BDF8))
                StatusRow("Pairing code", state.pairingId.ifBlank { "—" }, Color(0xFF6366F1))
                StatusRow("Relay", state.relayUrl.removePrefix("https://").take(38), Color(0xFF94A3B8))
            }
        }

        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenConnection, modifier = Modifier.fillMaxWidth()) {
                Text("Connection & Telegram (optional)")
            }
            OutlinedButton(
                onClick = {
                    try {
                        ctx.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                        try { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Battery optimization band karo (recommended)") }
            OutlinedButton(
                onClick = {
                    try { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Notification access settings") }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to black screen") }
            OutlinedButton(
                onClick = { vm.stopForwarding(); onReset() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
            ) { Text("Stop forwarding & re-setup") }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Note: forwarding Android ke background services se chalta hai (NotificationListenerService + WorkManager). Screen band/black hone se rukta nahi. Kuch OEMs aggressive app-killers use karte hain — battery optimization band karna best hai.",
            color = Color(0xFF64748B), fontSize = 11.sp
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String, tint: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(value, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
