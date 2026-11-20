package com.imista.forward.sender.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imista.forward.sender.data.database.AppDatabase
import com.imista.forward.sender.data.entity.AppFilterEntity
import com.imista.forward.sender.security.SecureStorage
import com.imista.forward.shared.model.ForwardingMode
import kotlinx.coroutines.launch

@Composable
fun AppSelectionScreen() {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val secure = remember { SecureStorage(ctx) }
    var apps by remember { mutableStateOf(listOf<AppInfo>()) }
    var mode by remember { mutableStateOf(secure.forwardingMode) }
    var filters by remember { mutableStateOf(setOf<String>()) }
    var excluded by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Load installed apps that can post notifications (simple: launchable apps)
        val pm = ctx.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { AppInfo(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
        apps = installed.take(200) // limit

        val dbFilters = db.filterDao().getAll()
        filters = dbFilters.filter { it.isEnabled }.map { it.packageName }.toSet()
        excluded = dbFilters.filter { it.isExcluded }.map { it.packageName }.toSet()
        mode = secure.forwardingMode
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("App Selection & Filtering", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Choose forwarding mode. Sender supports WhatsApp, SMS, Gmail, Outlook, Instagram, Telegram, Discord and any app posting notifications.", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = mode == "ALL_APPS", onClick = { mode = "ALL_APPS"; secure.forwardingMode = mode }, label = { Text("All Apps") }, modifier = Modifier.weight(1f))
            FilterChip(selected = mode == "SELECTED_ONLY", onClick = { mode = "SELECTED_ONLY"; secure.forwardingMode = mode }, label = { Text("Selected") }, modifier = Modifier.weight(1f))
            FilterChip(selected = mode == "EXCLUDE_LIST", onClick = { mode = "EXCLUDE_LIST"; secure.forwardingMode = mode }, label = { Text("Exclude") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text("Tap to toggle. In SELECTED mode only checked apps forward. In EXCLUDE mode checked apps are blocked.", fontSize = 12.sp, color = Color(0xFF94A3B8))
        Spacer(Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(apps) { app ->
                val isChecked = when (mode) {
                    "SELECTED_ONLY" -> filters.contains(app.pkg)
                    "EXCLUDE_LIST" -> excluded.contains(app.pkg)
                    else -> !excluded.contains(app.pkg) // All apps shows excluded unchecked meaning allow? Show inverse? simpler: show excluded as checked to exclude
                }
                Card(onClick = {
                    scope.launch {
                        when (mode) {
                            "SELECTED_ONLY" -> {
                                val now = if (filters.contains(app.pkg)) filters - app.pkg else filters + app.pkg
                                filters = now
                                db.filterDao().insert(AppFilterEntity(app.pkg, app.label, isEnabled = now.contains(app.pkg), isExcluded = false))
                            }
                            "EXCLUDE_LIST" -> {
                                val now = if (excluded.contains(app.pkg)) excluded - app.pkg else excluded + app.pkg
                                excluded = now
                                db.filterDao().insert(AppFilterEntity(app.pkg, app.label, isEnabled = false, isExcluded = now.contains(app.pkg)))
                            }
                            else -> { // ALL_APPS: toggle exclude
                                val now = if (excluded.contains(app.pkg)) excluded - app.pkg else excluded + app.pkg
                                excluded = now
                                db.filterDao().insert(AppFilterEntity(app.pkg, app.label, isEnabled = false, isExcluded = now.contains(app.pkg)))
                            }
                        }
                    }
                }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(app.label, fontWeight = FontWeight.Medium, fontSize = 14.sp); Text(app.pkg, fontSize = 11.sp, color = Color(0xFF64748B)) }
                        Checkbox(checked = isChecked, onCheckedChange = null)
                    }
                }
            }
        }
    }
}

data class AppInfo(val pkg: String, val label: String)
