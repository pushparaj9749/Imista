package com.imista.forward.sender.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.imista.forward.sender.viewmodel.SetupViewModel

@Composable
fun SetupScreen(vm: SetupViewModel = viewModel(), onDone: () -> Unit) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Imista Forward — Sender Setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text("Configure once, then it runs quietly in background. Token is kept on secure relay, never in app.", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        // Step 1: Notification Access
        StepCard(
            number = "1",
            title = "Notification Access",
            desc = "Allow reading notifications to forward them. Only title/text exposed by Android.",
            status = if (state.notificationEnabled) "Enabled ✅" else "Not enabled",
            positive = state.notificationEnabled,
            actionLabel = if (state.notificationEnabled) "Re-check" else "Enable",
            onAction = {
                try { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } catch (_: Exception) {}
            },
            onRefresh = { vm.refresh() }
        )

        Spacer(Modifier.height(12.dp))

        // Step 2: Device Admin (optional)
        StepCard(
            number = "2",
            title = "Device Admin (Optional)",
            desc = "Allows device lock only. Does not prevent uninstall. You can skip.",
            status = if (state.deviceAdminEnabled) "Active ✅" else "Inactive (optional)",
            positive = state.deviceAdminEnabled,
            actionLabel = "Manage",
            onAction = {
                try {
                    val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, android.content.ComponentName(ctx, com.imista.forward.sender.receiver.ImistaDeviceAdminReceiver::class.java))
                        putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Optional lock feature only. No data collection.")
                    }
                    ctx.startActivity(intent)
                } catch (_: Exception) {
                    ctx.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                }
            },
            onRefresh = { vm.refresh() }
        )

        Spacer(Modifier.height(12.dp))

        // Step 3: Pairing
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge("3"); Spacer(Modifier.width(10.dp)); Text("Device Pairing", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                Text("Pair this Sender with Receiver via secure relay. Relay holds Telegram token server-side. No manual token entry.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.pairingId,
                    onValueChange = { vm.updatePairingId(it) },
                    label = { Text("Pairing ID (UUID)") },
                    placeholder = { Text("auto-generated") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text("Relay: ${state.relayUrl}", fontSize = 11.sp, color = Color(0xFF64748B))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { vm.generateNewPairing() }, modifier = Modifier.weight(1f)) { Text("Generate New") }
                    Button(onClick = { vm.registerPairing() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))) { Text("Register") }
                }
                Spacer(Modifier.height(8.dp))
                Text("Share this Pairing ID with Receiver (copy or QR). Receiver enters same ID to join same relay channel.", fontSize = 12.sp, color = Color(0xFFFBBF24))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Step 4: Connection verification
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge("4"); Spacer(Modifier.width(10.dp)); Text("Connection Verification", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.verifyConnection() }, modifier = Modifier.fillMaxWidth(), enabled = !state.isVerifying) {
                    if (state.isVerifying) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Verify Secure Relay")
                }
                state.verificationStatus?.let {
                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = if (it.contains("✅")) Color(0xFF064E3B) else Color(0xFF1E293B))) {
                        Text(it, modifier = Modifier.padding(10.dp), color = Color.White, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Relay holds TELEGRAM_BOT_KEY server-side (from GitHub Secret). APK never contains token. Both apps use same relay → Telegram. Handles rate-limit, retry, polling offsets without competing consumers.", fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Step 5: Start Forwarding
        Button(
            onClick = {
                vm.startForwarding()
                onDone()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = state.canStartForwarding && !state.isVerifying,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Start Forwarding", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text("After starting, screen turns black with minimal indicator. Forwarding works in background via supported WorkManager & NotificationListener, not by keeping screen open. Handle background restrictions: exclude from battery optimization.", fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF422006))) {
            Text("Note: Android never guarantees permanent background execution. Offline queue with exponential backoff handles restarts, permission revocation, and rate-limits. Forwarding remains usable without Device Admin.", modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = Color(0xFFFBBF24))
        }
    }
}

@Composable
private fun StepCard(number: String, title: String, desc: String, status: String, positive: Boolean, actionLabel: String, onAction: () -> Unit, onRefresh: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(number)
                    Spacer(Modifier.width(10.dp))
                    Text(title, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Text(status, fontSize = 12.sp, color = if (positive) Color(0xFF10B981) else Color(0xFFEF4444), fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
            Text(desc, fontSize = 13.sp, color = Color(0xFF94A3B8))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onAction, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))) { Text(actionLabel) }
                OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(0.6f)) { Text("Refresh") }
            }
        }
    }
}

@Composable
private fun Badge(num: String) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1))) {
        Text(num, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
