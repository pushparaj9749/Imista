package com.imista.forward.sender.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("About Imista Forward Sender", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("v1.0.0 • Package: com.imista.forward.sender", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Text("Imista Forward is a private phone-to-phone notification forwarder using your own Telegram bot as relay — no Firebase, no Supabase, no custom backend. Sender captures notifications via Android NotificationListenerService and forwards to your private Telegram chat.", fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
            Column(Modifier.padding(14.dp)) {
                Text("Permissions", fontWeight = FontWeight.SemiBold)
                Text("• Notification Access: read notifications you allow\n• Internet: send to Telegram API via HTTPS\n• Device Admin (optional): lock device only\n• No location, contacts, or SMS reading beyond notifications", fontSize = 13.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top=6.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Open Source & Privacy", fontWeight = FontWeight.SemiBold)
        Text("• No analytics, no ads, no third-party collection\n• Credentials stored via Keystore\n• Notification content never logged\n• Local retention configurable, auto-prune 7 days", fontSize = 13.sp, color = Color(0xFF94A3B8))
        Spacer(Modifier.height(12.dp))
        Text("Known Limitations", fontWeight = FontWeight.SemiBold)
        Text("• Android may delay background work under battery optimization\n• Telegram Bot API rate limits (≈30 msgs/sec) with Retry-After handling\n• Duplicate suppression window 7s\n• Receiver reconstruction depends on Telegram notification settings if using Telegram-notification parsing mode", fontSize = 13.sp, color = Color(0xFF94A3B8))
        Spacer(Modifier.height(12.dp))
        Text("© 2026 Imista • Built with Kotlin, Compose, Room, WorkManager", fontSize = 12.sp, color = Color(0xFF64748B))
    }
}
