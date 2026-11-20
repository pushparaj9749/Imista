package com.imista.forward.sender.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imista.forward.sender.data.database.AppDatabase
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen() {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    var history by remember { mutableStateOf(listOf<com.imista.forward.sender.data.entity.DeliveryHistoryEntity>()) }
    var pending by remember { mutableStateOf(listOf<com.imista.forward.sender.data.entity.PendingNotificationEntity>()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        history = db.historyDao().getRecent(100)
        pending = db.pendingDao().getRecent(100)
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Delivery History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Shows last 100 deliveries. Pending queue retries with exponential backoff and respects Telegram rate-limits (retry_after).", color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        if (pending.isNotEmpty()) {
            Text("Pending / Retrying (${pending.size})", fontWeight = FontWeight.SemiBold)
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(pending) { p ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF422006)), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Column(Modifier.padding(8.dp)) { Text(p.appName, fontWeight = FontWeight.Medium, fontSize = 13.sp); Text("${p.title} • ${p.status} (${p.attempts})", fontSize = 12.sp, color = Color(0xFF94A3B8)) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Text("Recent Delivered", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(history) { h ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(h.appName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(h.status, fontSize = 11.sp, color = if (h.status == "SENT") Color(0xFF10B981) else Color(0xFFEF4444))
                        }
                        Text(h.title ?: "", fontSize = 13.sp, color = Color(0xFFE2E8F0))
                        Text(java.text.SimpleDateFormat("dd MMM HH:mm").format(java.util.Date(h.deliveredAt)), fontSize = 11.sp, color = Color(0xFF64748B))
                        if (h.error != null) Text(h.error, fontSize = 11.sp, color = Color(0xFFEF4444))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { scope.launch { db.historyDao().clearAll(); history = emptyList() } }, modifier = Modifier.fillMaxWidth()) { Text("Clear History") }
    }
}
