package com.imista.forward.sender.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotificationAccessScreen() {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Notification Access", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Imista Sender needs Notification Listener access to read incoming notifications from apps you choose. It only forwards what Android exposes via NotificationListenerService — no private databases.", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
            Column(Modifier.padding(16.dp)) {
                Text("What is accessed?", fontWeight = FontWeight.SemiBold)
                Text("• App name & package\n• Title & text shown in notification\n• Timestamp & category\n• No hidden message content beyond what Android shows", fontSize = 13.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top=8.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            try { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } catch (_: Exception) {}
        }, modifier = Modifier.fillMaxWidth()) { Text("Open Notification Access Settings") }
        Spacer(Modifier.height(12.dp))
        Text("After enabling, return here. The service will show 'Connected' on Dashboard. If disabled later, forwarding pauses until re-enabled.", fontSize = 12.sp, color = Color(0xFF94A3B8))
        Spacer(Modifier.height(20.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF422006))) {
            Text("Note: Some manufacturers (Xiaomi, Samsung, Oppo) aggressively kill background services. Exclude Imista from battery optimization for reliability. Android never guarantees permanent process alive — retry queue handles offline periods.", modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = Color(0xFFFBBF24))
        }
    }
}
