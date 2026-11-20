package com.imista.forward.receiver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imista.forward.receiver.data.database.AppDatabase
import com.imista.forward.receiver.ui.components.StatusCard
import com.imista.forward.receiver.viewmodel.HomeViewModel

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel(), onNavigate: (String)->Unit) {
    val state by vm.state.collectAsState()
    val recent by vm.recent.collectAsState(initial = emptyList())
    LaunchedEffect(Unit) { vm.refresh() }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))) { Text("IF", modifier = Modifier.padding(horizontal=14.dp, vertical=8.dp), color = Color.White, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(12.dp))
                        Column { Text("Imista Forward", color = Color.White, fontWeight = FontWeight.Bold, fontSize=18.sp); Text("Receiver • Private Inbox", color = Color.White.copy(alpha=0.8f), fontSize=13.sp) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(if (state.telegramConnected) "● Listening for Telegram forwards" else "● Configure Telegram to start", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusCard("Telegram", if (state.telegramConnected) "Connected" else "Setup needed", state.telegramConnected, modifier = Modifier.weight(1f))
                StatusCard("Listener", if (state.isListenerEnabled) "Enabled" else "Optional", state.isListenerEnabled, modifier = Modifier.weight(1f))
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF151521)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Inbox Overview", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("${state.totalReceived}", fontSize=24.sp, fontWeight=FontWeight.Bold, color=Color(0xFF6366F1)); Text("Total received", fontSize=12.sp, color=Color(0xFF94A3B8)) }
                        Column { Text("${state.unread}", fontSize=24.sp, fontWeight=FontWeight.Bold, color=Color(0xFF06B6D4)); Text("Unread", fontSize=12.sp, color=Color(0xFF94A3B8)) }
                        Column { Text(state.lastReceived?.let { java.text.SimpleDateFormat("HH:mm").format(java.util.Date(it)) } ?: "--:--", fontSize=16.sp, fontWeight=FontWeight.SemiBold); Text("Last received", fontSize=12.sp, color=Color(0xFF94A3B8)) }
                    }
                }
            }
        }
        item {
            Text("Quick Actions", fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickRow("Connection Setup", "Bot token & Chat ID", Icons.Default.Link) { onNavigate("connection") }
                QuickRow("Notification Feed", "Browse forwarded content", Icons.Default.Inbox) { onNavigate("feed") }
                QuickRow("App Filters", "Filter by app", Icons.Default.FilterList) { onNavigate("filters") }
                QuickRow("Privacy", "Retention & preview", Icons.Default.Lock) { onNavigate("privacy") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Notifications", fontWeight=FontWeight.SemiBold)
                TextButton(onClick = { onNavigate("feed") }) { Text("View all") }
            }
        }
        if (recent.isEmpty()) {
            item { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)), modifier = Modifier.fillMaxWidth()) { Text("No notifications yet. Ensure Sender is forwarding and your Telegram notifications are enabled for the bot.", modifier = Modifier.padding(16.dp), fontSize=13.sp, color=Color(0xFF94A3B8)) } }
        } else {
            items(recent.take(5)) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.appName, fontWeight = FontWeight.Medium, fontSize=14.sp, color=Color(0xFF6366F1))
                            Text(java.text.SimpleDateFormat("HH:mm dd MMM").format(java.util.Date(item.timestamp)), fontSize=11.sp, color=Color(0xFF64748B))
                        }
                        Text(item.title ?: "", fontSize=14.sp, fontWeight=FontWeight.Medium)
                        Text(item.text ?: "", fontSize=13.sp, color=Color(0xFFCBD5E1), maxLines = 2)
                        if (!item.isRead) Text("● Unread", fontSize=11.sp, color=Color(0xFF10B981))
                    }
                }
            }
        }
        item { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) { Text("ℹ️ Receiver uses Telegram notification listening to reconstruct forwarded messages. It does NOT use Firebase. Keep Telegram notifications enabled for your bot. Bot messages are not Secret Chats.", modifier=Modifier.padding(12.dp), fontSize=12.sp, color=Color(0xFF94A3B8)) } }
    }
}
@Composable private fun QuickRow(title:String, sub:String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick:()->Unit) {
    Card(onClick = onClick, shape=RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint=Color(0xFF6366F1)); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight=FontWeight.Medium, fontSize=15.sp); Text(sub, fontSize=12.sp, color=Color(0xFF94A3B8)) }
            Icon(Icons.Default.ChevronRight, null, tint=Color(0xFF64748B))
        }
    }
}
