package com.imista.forward.sender.ui.screens

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imista.forward.sender.viewmodel.SetupViewModel
import kotlinx.coroutines.delay

/**
 * The only screen the user sees after setup: a true-black, battery-friendly screen.
 *
 * No gear, no buttons, no navigation. Settings are reachable only by a deliberate
 * 6-tap pattern on the centre logo (so a curious friend can't wander into setup).
 * Forwarding keeps running regardless of this screen — the UI does not do any work.
 */
@Composable
fun BlackScreen(vm: SetupViewModel = viewModel(), onOpenSettings: () -> Unit) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var dots by remember { mutableStateOf(1) }
    var batteryPct by remember { mutableStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    var screenOn by remember { mutableStateOf(true) }
    var powerSave by remember { mutableStateOf(false) }
    var lastForwardText by remember { mutableStateOf("—") }
    var totalForwarded by remember { mutableStateOf(0) }
    var taps by remember { mutableStateOf(0) }

    // Pulse animation for the indicator (cheap, does not keep the CPU awake).
    LaunchedEffect(Unit) {
        while (true) {
            delay(1200)
            dots = (dots % 3) + 1
            batteryPct = readBattery(context)
            charging = isCharging(context)
            powerSave = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
            screenOn = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive == true
            val status = context.getSharedPreferences("imista_status", Context.MODE_PRIVATE)
            val last = status.getLong("last_forward", 0L)
            lastForwardText = if (last == 0L) "abhi tak kuch nahi" else agoText(last)
            totalForwarded = status.getInt("total_forwarded", 0)
        }
    }

    // Tapping the logo 6 times quickly opens the settings. Deliberately obscure.
    LaunchedEffect(taps) {
        if (taps > 0 && taps < 6) {
            delay(1500)
            taps = 0
        } else if (taps >= 6) {
            taps = 0
            onOpenSettings()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "IMISTA",
                color = Color.White.copy(alpha = 0.10f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 6.sp,
                modifier = Modifier.clickable { taps += 1 }
            )
            Spacer(Modifier.height(26.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                PulsingDot(active = state.notificationEnabled && state.forwardingActive)
                Text(
                    when {
                        !state.notificationEnabled -> "Notification access band hai"
                        !state.forwardingActive -> "Forwarding band hai"
                        else -> "Chal raha hai${".".repeat(dots)}"
                    },
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(28.dp))
            // Very dim, small status block — readable only when you look for it.
            Column(
                Modifier
                    .background(Color(0xFF0A0A0A), RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MiniStat(
                        label = "Battery",
                        value = if (batteryPct >= 0) "$batteryPct%" else "—",
                        tint = when {
                            batteryPct in 0..15 -> Color(0xFFEF4444)
                            charging -> Color(0xFF10B981)
                            else -> Color.White.copy(alpha = 0.5f)
                        }
                    )
                    MiniStat(
                        label = "Charging",
                        value = if (charging) "Yes" else "No",
                        tint = if (charging) Color(0xFF10B981) else Color.White.copy(alpha = 0.35f)
                    )
                    MiniStat(
                        label = "Forwarded",
                        value = totalForwarded.toString(),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Last forward: $lastForwardText",
                    color = Color.White.copy(alpha = 0.28f),
                    fontSize = 10.sp
                )
                if (powerSave) {
                    Spacer(Modifier.height(4.dp))
                    Text("Battery saver ON — delayed forwarding possible", color = Color(0xFFF59E0B).copy(alpha = 0.6f), fontSize = 9.sp)
                }
            }
        }

        // Extremely discreet footer: pairing proof, no secrets.
        Text(
            "relay • ${state.pairingId.ifBlank { "unpaired" }}",
            color = Color.White.copy(alpha = 0.10f),
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
        )
        if (!screenOn) Unit // no-op; keeps the value used for future telemetry
    }
}

@Composable
private fun PulsingDot(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        Modifier
            .size(8.dp)
            .background(
                color = (if (active) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = if (active) alpha else 0.55f),
                shape = CircleShape
            )
    )
}

@Composable
private fun MiniStat(label: String, value: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = tint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Color.White.copy(alpha = 0.22f), fontSize = 9.sp)
    }
}

private fun readBattery(ctx: Context): Int {
    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 } ?: -1
}

private fun isCharging(ctx: Context): Boolean {
    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    return bm?.isCharging == true
}

private fun agoText(ts: Long): String {
    val mins = (System.currentTimeMillis() - ts) / 60000
    return when {
        mins < 1 -> "abhi"
        mins < 60 -> "${mins}m pehle"
        mins < 1440 -> "${mins / 60}h pehle"
        else -> "${mins / 1440}d pehle"
    }
}
