package com.imista.forward.sender.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imista.forward.sender.ui.components.StatusCard
import com.imista.forward.sender.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    vm: DashboardViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))) {
                            Text("IF", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Imista Forward", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Sender • Private Relay", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(if (state.isTelegramConnected && state.isListenerEnabled) "● Forwarding Active" else "● Setup Required", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusCard("Listener", if (state.isListenerEnabled) "Enabled" else "Disabled", state.isListenerEnabled, modifier = Modifier.weight(1f))
                StatusCard("Telegram", if (state.isTelegramConnected) "Connected" else "Not set", state.isTelegramConnected, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusCard("Device Admin", if (state.isDeviceAdminEnabled) "Active" else "Inactive", state.isDeviceAdminEnabled, modifier = Modifier.weight(1f))
                StatusCard("Pending Queue", "${state.pendingCount}", state.pendingCount == 0, modifier = Modifier.weight(1f))
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF151521)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Today's Activity", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("${state.notificationsToday}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1)); Text("Forwarded today", fontSize = 12.sp, color = Color(0xFF94A3B8)) }
                        Column { Text(state.lastDelivery?.let { java.text.SimpleDateFormat("HH:mm").format(java.util.Date(it)) } ?: "--:--", fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text("Last delivery", fontSize = 12.sp, color = Color(0xFF94A3B8)) }
                    }
                }
            }
        }
        item {
            Text("Quick Actions", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Connection & Telegram", "Pairing status (server-side token)", Icons.Default.Send) { onNavigate("telegram") }
                QuickAction("Notification Access", "Allow reading notifications", Icons.Default.Notifications) { onNavigate("notification_access") }
                QuickAction("App Selection", "Choose apps to forward", Icons.Default.Apps) { onNavigate("app_selection") }
                QuickAction("Device Admin", "Optional protection", Icons.Default.Security) { onNavigate("device_admin") }
                QuickAction("Delivery History", "Recent forwards", Icons.Default.History) { onNavigate("history") }
                QuickAction("Privacy & Security", "Encryption & retention", Icons.Default.Lock) { onNavigate("privacy") }
            }
        }
        item {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
                Text("🔒 Telegram messages are not end-to-end encrypted. Enable app-level encryption in Privacy settings for sensitive content. Notification forwarding works even without Device Admin.", modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}

@Composable
private fun QuickAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF6366F1))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp); Text(subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8)) }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B))
        }
    }
}
