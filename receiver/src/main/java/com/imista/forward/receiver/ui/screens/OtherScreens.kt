package com.imista.forward.receiver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FiltersScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Application Filters", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Filter feed by app using chips on Feed screen. Future: per-app mute and hide.", color=Color(0xFF94A3B8), fontSize=13.sp)
        Spacer(Modifier.height(16.dp))
        Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF1E1E2F))) { Text("Current filters are display-only; forwarding filters are controlled on Sender (App Selection). Receiver filters only affect local viewing, not delivery.", modifier=Modifier.padding(14.dp), fontSize=13.sp, color=Color(0xFF94A3B8)) }
    }
}

@Composable
fun PrivacyReceiverScreen() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("imista_receiver_prefs", 0) }
    var mirror by remember { mutableStateOf(prefs.getBoolean("mirror_local", false)) }
    var retention by remember { mutableStateOf(prefs.getInt("retention_days", 7)) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Privacy Settings", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF1E1E2F))) { Column(Modifier.padding(14.dp)) {
            Text("Telegram Bot messages are HTTPS-encrypted in transit but not Telegram Secret Chat E2E. Enable Sender-side encryption for sensitive apps.", fontSize=13.sp, color=Color(0xFFF59E0B))
        }}
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Mirror as local notification", fontWeight=FontWeight.Medium); Text("Re-post forwarded content as phone notification", fontSize=12.sp, color=Color(0xFF94A3B8)) }
            Switch(checked=mirror, onCheckedChange={ mirror=it; prefs.edit().putBoolean("mirror_local", it).apply() })
        }
        Spacer(Modifier.height(12.dp))
        Text("Local retention: $retention days", fontWeight=FontWeight.Medium)
        Slider(value=retention.toFloat(), onValueChange={ retention=it.toInt(); prefs.edit().putInt("retention_days", retention).apply() }, valueRange=1f..30f, steps=5)
        Spacer(Modifier.height(12.dp))
        Text("No analytics, no ads. Clearing history deletes local DB. Bot token stored via Keystore.", fontSize=13.sp, color=Color(0xFF94A3B8))
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick={
            prefs.edit().clear().apply()
            // also clear secure storage
            com.imista.forward.receiver.security.SecureStorage(ctx).clearAll()
        }, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.outlinedButtonColors(contentColor=Color(0xFFEF4444))) { Text("Disconnect & Clear Credentials") }
    }
}

@Composable
fun HistoryReceiverScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Delivery History", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Shown in Feed. Receiver does not have separate delivery status; delivery status is visible in Sender's History. Receiver retention auto-prunes after configured days.", color=Color(0xFF94A3B8))
    }
}

@Composable
fun AboutReceiverScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("About Imista Forward Receiver", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("v1.0.0 • com.imista.forward.receiver", color=Color(0xFF94A3B8), fontSize=13.sp)
        Spacer(Modifier.height(12.dp))
        Text("Receiver displays notifications forwarded via your Telegram bot. It reconstructs messages from Telegram app notifications — a valid architecture given Bot API limitations (no push to another app). Requires Telegram notifications enabled.", fontSize=14.sp)
        Spacer(Modifier.height(12.dp))
        Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF1E1E2F))) { Column(Modifier.padding(14.dp)) { Text("Permissions", fontWeight=FontWeight.SemiBold); Text("• Internet: verify bot via HTTPS\n• Notification Access (optional): capture Telegram notifications for reconstruction\n• No Device Admin needed on Receiver\n• No location/contacts", fontSize=13.sp, color=Color(0xFF94A3B8), modifier=Modifier.padding(top=6.dp)) } }
        Spacer(Modifier.height(12.dp))
        Text("Limitations", fontWeight=FontWeight.SemiBold)
        Text("• Depends on Telegram notification settings if using notification reconstruction\n• Only one app should poll getUpdates to avoid stealing updates — Sender never polls\n• Local mirroring must avoid loops (we filter our own package)", fontSize=13.sp, color=Color(0xFF94A3B8))
        Spacer(Modifier.height(12.dp))
        Text("© 2026 Imista", fontSize=12.sp, color=Color(0xFF64748B))
    }
}

@Composable
fun TelegramListenerScreen() {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Telegram Listener", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Enable Notification Access so Receiver can capture Telegram notifications containing forwarded messages. This is how Receiver obtains Sender's forwards without Firebase.", color=Color(0xFF94A3B8), fontSize=13.sp)
        Spacer(Modifier.height(16.dp))
        Button(onClick={
            try { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } catch (_:Exception) {}
        }, modifier=Modifier.fillMaxWidth()) { Text("Open Notification Access Settings") }
        Spacer(Modifier.height(12.dp))
        Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF422006))) { Text("After enabling, send a test notification from Sender. Verify it appears in Telegram and then in Receiver Feed. If Telegram notification is disabled or filtered, Receiver will not see it.", modifier=Modifier.padding(12.dp), fontSize=12.sp, color=Color(0xFFFBBF24)) }
    }
}
