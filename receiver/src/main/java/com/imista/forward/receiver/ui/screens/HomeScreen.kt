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
                    Text(if (state.telegramConnected) "● Relay connected — receiving forwards" else "● Paired nahi hai — Connect Receiver dabao", color = Color.White, fontWeight = FontWeight.SemiBold)
                    state.pairingCode?.let {
                        Spacer(Modifier.height(6.dp))
                        Text("Pairing code: $it", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    state.lastError?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("Last error: $it", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusCard("Relay", if (state.telegramConnected) "Connected" else "Pairing needed", state.telegramConnected, modifier = Modifier.weight(1f))
                StatusCard("Listener", if (state.isListenerEnabled) "Enabled" else "Optional", state.isListenerEnabled, modifier = Modifier.weight(1f))
            }
        }
        item {
            val s = state.sender
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF151521)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Sender phone", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (s == null) "no data yet" else if (state.senderOnline) "● live" else "● offline",
                            fontSize = 11.sp,
                            color = if (state.senderOnline) Color(0xFF10B981) else Color(0xFF94A3B8)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (s == null) {
                        Text(
                            "Sender ne status bheja nahi hai. Uska app ek baar kholo — heartbeat apne aap chalu ho jayega.",
                            fontSize = 12.sp, color = Color(0xFF94A3B8)
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MiniTile("Battery",
                                if ((s.batteryPct ?: -1) >= 0) "${s.batteryPct}%" else "—",
                                when {
                                    (s.batteryPct ?: 100) <= 15 -> Color(0xFFEF4444)
                                    s.charging == true -> Color(0xFF10B981)
                                    else -> Color(0xFF6366F1)
                                }, Modifier.weight(1f))
                            MiniTile("Charging", if (s.charging == true) "Yes" else "No",
                                if (s.charging == true) Color(0xFF10B981) else Color(0xFF475569), Modifier.weight(1f))
                            MiniTile("Network", s.network ?: "—", Color(0xFF38BDF8), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MiniTile("Forwarding", if (s.forwarding) "ON" else "OFF",
                                if (s.forwarding) Color(0xFF10B981) else Color(0xFFEF4444), Modifier.weight(1f))
                            MiniTile("Forwarded", (s.forwardedTotal ?: 0).toString(), Color(0xFF6366F1), Modifier.weight(1f))
                            MiniTile("Storage", "${s.storageFreeMb ?: 0}MB", Color(0xFF94A3B8), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${s.deviceModel ?: "Sender"} • Android ${s.appVersion ?: "?"} • " + ageText(state.senderUpdatedAt),
                            fontSize = 10.sp, color = Color(0xFF64748B)
                        )
                        if (s.notificationAccess.not() || s.forwarding.not()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "⚠️ Sender pe " + (if (!s.notificationAccess) "notification access band hai" else "forwarding band hai"),
                                fontSize = 11.sp, color = Color(0xFFF59E0B)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { vm.refreshSenderStatus() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Refresh sender status")
                    }
                }
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
                QuickRow("Connect Receiver", "6-digit code se pairing", Icons.Default.Link) { onNavigate("connection") }
                QuickRow("Sender Status", "Battery, network, forwarding", Icons.Default.BatteryFull) { onNavigate("sender_status") }
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
            item { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)), modifier = Modifier.fillMaxWidth()) { Text("Abhi kuch nahi aaya. Sender pe forwarding ON hai na? Code same hai na? Phir bhi na aaye to Connect screen se \"Fetch now\" dabao.", modifier = Modifier.padding(16.dp), fontSize=13.sp, color=Color(0xFF94A3B8)) } }
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

@Composable
private fun MiniTile(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(vertical = 10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = tint, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(label, color = Color(0xFF64748B), fontSize = 9.sp)
        }
    }
}

private fun ageText(ts: Long?): String {
    if (ts == null) return "unknown"
    val mins = (System.currentTimeMillis() - ts) / 60000
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins} min ago"
        mins < 1440 -> "${mins / 60}h ago"
        else -> "${mins / 1440}d ago"
    }
}
