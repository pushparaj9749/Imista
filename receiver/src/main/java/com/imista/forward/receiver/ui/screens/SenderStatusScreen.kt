package com.imista.forward.receiver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imista.forward.receiver.viewmodel.HomeViewModel
import com.imista.forward.shared.network.StatusInfo

/**
 * Everything the Receiver knows about the Sender phone: battery, charging, network,
 * forwarding health, uptime and last heartbeat. Data comes from the relay heartbeat,
 * so it works even when the Sender app is closed / its screen is black.
 */
@Composable
fun SenderStatusScreen(vm: HomeViewModel = viewModel(), onBack: () -> Unit) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.refreshSenderStatus() }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Sender phone status", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Sender har 15 min me apna status relay pe bhejta hai (notification aane pe turant). App band ho to bhi chalta hai.",
            color = Color(0xFF94A3B8), fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))

        val s: StatusInfo? = state.sender
        if (s == null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Abhi koi status nahi aaya", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Sender phone pe app ek baar kholo (black screen tak). Uske baad heartbeat apne aap chalta rahega.",
                        fontSize = 12.sp, color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigTile("Battery", if ((s.batteryPct ?: -1) >= 0) "${s.batteryPct}%" else "—",
                    when {
                        (s.batteryPct ?: 100) <= 15 -> Color(0xFFEF4444)
                        (s.batteryPct ?: 100) <= 40 -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    }, Modifier.weight(1f))
                BigTile("Charging", if (s.charging == true) "Yes" else "No",
                    if (s.charging == true) Color(0xFF10B981) else Color(0xFF64748B), Modifier.weight(1f))
                BigTile("Network", s.network ?: "—", Color(0xFF38BDF8), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigTile("Forwarding", if (s.forwarding) "ON" else "OFF",
                    if (s.forwarding) Color(0xFF10B981) else Color(0xFFEF4444), Modifier.weight(1f))
                BigTile("Sent", (s.forwardedTotal ?: 0).toString(), Color(0xFF6366F1), Modifier.weight(1f))
                BigTile("Uptime", "${(s.uptimeMinutes ?: 0) / 60}h", Color(0xFF94A3B8), Modifier.weight(1f))
            }

            Spacer(Modifier.height(14.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151521)), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Details", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row2("Device", s.deviceModel ?: "—")
                    Row2("Android", s.appVersion ?: "—")
                    Row2("Screen", if (s.screenOn == true) "On" else "Off")
                    Row2("Screen locked", if (s.screenLocked == true) "Yes" else "No")
                    Row2("Battery saver", if (s.batterySaver == true) "ON" else "Off")
                    Row2("Notification access", if (s.notificationAccess) "Granted" else "Missing")
                    Row2("Free storage", "${s.storageFreeMb ?: 0} MB")
                    Row2("Last heartbeat", agoText(state.senderUpdatedAt ?: s.updatedAt))
                    Row2("Link", if (state.senderOnline) "Live" else "Stale (>45 min)")
                }
            }

            if (!s.forwarding || !s.notificationAccess) {
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF422006)), shape = RoundedCornerShape(14.dp)) {
                    Text(
                        "⚠️ Sender pe " + (if (!s.notificationAccess) "Notification Access band hai — kuch forward nahi hoga." else "forwarding band hai.") +
                            " Sender phone kholo, setup dobara chalao.",
                        modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = Color(0xFFFFE0A3)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.refreshSenderStatus() }, modifier = Modifier.fillMaxWidth()) { Text("Refresh") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun BigTile(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(vertical = 14.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = tint, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, color = Color(0xFF64748B), fontSize = 10.sp)
        }
    }
}

@Composable
private fun Row2(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(value, color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun agoText(ts: Long?): String {
    if (ts == null) return "unknown"
    val mins = (System.currentTimeMillis() - ts) / 60000
    return when {
        mins < 1 -> "abhi abhi"
        mins < 60 -> "$mins min pehle"
        mins < 1440 -> "${mins / 60} ghante pehle"
        else -> "${mins / 1440} din pehle"
    }
}
