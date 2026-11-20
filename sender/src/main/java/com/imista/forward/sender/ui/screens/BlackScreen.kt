package com.imista.forward.sender.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import com.imista.forward.sender.viewmodel.SetupViewModel
import kotlinx.coroutines.delay

@Composable
fun BlackScreen(vm: SetupViewModel = viewModel(), onOpenSettings: () -> Unit) {
    val state by vm.state.collectAsState()
    var isForwarding by remember { mutableStateOf(true) }
    var dots by remember { mutableStateOf(1) }

    // Subtle pulsing indicator
    LaunchedEffect(Unit) {
        while (true) {
            delay(900)
            dots = (dots % 3) + 1
            isForwarding = !isForwarding // toggle for subtle pulse
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) {}
    ) {
        // Center content: minimal label + status indicator
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("IMISTA FORWARD", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(if (vm.state.value.notificationEnabled) Color(0xFF10B981) else Color(0xFFEF4444), shape = CircleShape)
                )
                Text(
                    if (vm.state.value.notificationEnabled) "Forwarding${".".repeat(dots)}" else "Paused — permission revoked",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (vm.state.value.notificationEnabled) "Background • Tap gear to reconfigure" else "Enable Notification Access in settings",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
        }

        // Bottom discreet info
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Pairing: ${state.pairingId.take(8)}…", color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Text("Relay secured • Token server-side", color = Color.White.copy(alpha = 0.25f), fontSize = 9.sp)
        }

        // Top-right discreet settings gear
        IconButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.TopEnd).padding(top = 32.dp, end = 12.dp)) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun MinimalSettingsScreen(vm: SetupViewModel = viewModel(), onBack: () -> Unit, onReset: () -> Unit) {
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().background(Color(0xFF0B0B14)).padding(16.dp)) {
        Text("Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("Lightweight reconfiguration. Forwarding runs in background via system services, not this screen.", color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Status", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Pairing: ${state.pairingId}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text("Relay: ${state.relayUrl}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text("Notification Access: ${if (state.notificationEnabled) "Granted" else "Missing"}", color = if (state.notificationEnabled) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 12.sp)
                Text("Device Admin: ${if (state.deviceAdminEnabled) "Active" else "Inactive"}", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to Black Screen") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = {
            vm.stopForwarding()
            onReset()
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))) {
            Text("Stop Forwarding & Reconfigure")
        }
        Spacer(Modifier.height(16.dp))
        Text("Note: Stopping clears onboarding flag. Forwarding uses Android background architecture (NotificationListenerService + WorkManager). Locking phone is fine. Do not claim guaranteed permanent execution.", color = Color(0xFF94A3B8), fontSize = 11.sp)
    }
}
