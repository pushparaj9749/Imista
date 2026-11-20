package com.imista.forward.receiver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imista.forward.receiver.data.database.AppDatabase
import kotlinx.coroutines.launch

@Composable
fun FeedScreen() {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    var query by remember { mutableStateOf("") }
    var filterApp by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf(listOf<com.imista.forward.receiver.data.entity.ReceivedNotificationEntity>()) }
    var appsDistinct by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query, filterApp) {
        // For simplicity reload all and filter locally
        val all = db.receivedDao().getRecent(500)
        var filtered = all
        if (query.isNotBlank()) filtered = filtered.filter { it.appName.contains(query, true) || (it.title?.contains(query, true) == true) || (it.text?.contains(query, true) == true) }
        if (filterApp != null) filtered = filtered.filter { it.appName == filterApp }
        items = filtered
        appsDistinct = all.map { it.appName }.distinct().sorted()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Notification Feed", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value=query, onValueChange={query=it}, label={Text("Search")}, leadingIcon={Icon(Icons.Default.Search, null)}, modifier=Modifier.fillMaxWidth(), singleLine=true)
        Spacer(Modifier.height(8.dp))
        if (appsDistinct.isNotEmpty()) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(selected=filterApp==null, onClick={filterApp=null}, label={Text("All")})
                appsDistinct.take(4).forEach { app ->
                    FilterChip(selected=filterApp==app, onClick={filterApp= if(filterApp==app) null else app}, label={Text(app.take(12))})
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp), modifier=Modifier.weight(1f)) {
            if (items.isEmpty()) {
                item { Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF1E1E2F)), modifier=Modifier.fillMaxWidth()) { Text("No notifications. Check Sender forwarding and Telegram notification settings.", modifier=Modifier.padding(16.dp), color=Color(0xFF94A3B8)) } }
            }
            items(items) { item ->
                Card(colors=CardDefaults.cardColors(containerColor= if(item.isRead) Color(0xFF1E1E2F) else Color(0xFF252540)), shape=RoundedCornerShape(14.dp), modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                            Text(item.appName, fontWeight=FontWeight.SemiBold, color=Color(0xFF6366F1), fontSize=14.sp)
                            Text(java.text.SimpleDateFormat("HH:mm dd MMM").format(java.util.Date(item.timestamp)), fontSize=11.sp, color=Color(0xFF64748B))
                        }
                        if (!item.title.isNullOrBlank()) Text(item.title, fontWeight=FontWeight.Medium, fontSize=15.sp)
                        if (!item.text.isNullOrBlank()) Text(item.text, fontSize=13.sp, color=Color(0xFFCBD5E1), maxLines=4)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            if (!item.isRead) AssistChip(onClick={ scope.launch { db.receivedDao().markRead(item.id); items = db.receivedDao().getRecent(500) } }, label={Text("Mark read")})
                            Text(item.packageName, fontSize=11.sp, color=Color(0xFF64748B))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp), modifier=Modifier.fillMaxWidth()) {
            OutlinedButton(onClick={ scope.launch { db.receivedDao().markAllRead(); items = db.receivedDao().getRecent(500) } }, modifier=Modifier.weight(1f)) { Text("Mark all read") }
            OutlinedButton(onClick={ scope.launch { db.receivedDao().clearAll(); items = emptyList() } }, modifier=Modifier.weight(1f), colors=ButtonDefaults.outlinedButtonColors(contentColor=Color(0xFFEF4444))) { Text("Clear") }
        }
    }
}
