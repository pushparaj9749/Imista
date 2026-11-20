package com.imista.forward.sender.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imista.forward.sender.viewmodel.SetupViewModel

/**
 * Connection status screen.
 *
 * The Telegram bot token is NOT configurable from the app — it lives only in the
 * relay's server secrets. Here the user can copy the pairing code, optionally
 * auto-link their Telegram chat with one tap, and see relay health.
 */
@Composable
fun TelegramConfigScreen(vm: SetupViewModel = viewModel(), onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.loadBotInfo() }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Connection", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Bot token sirf relay server pe rehta hai — app me kahin type nahi karna. Pairing ke liye bas 6-digit code.",
            color = Color(0xFF94A3B8), fontSize = 13.sp
        )

        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Pairing code", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    state.pairingId.ifBlank { "— (Generate dabao)" },
                    fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8)
                )
                Spacer(Modifier.height(8.dp))
                Text("Receiver me yahi code daalo. Dono devices jud jayenge.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.generateNewPairing() }, modifier = Modifier.weight(1f), enabled = !state.isVerifying) {
                        Text("Naya code")
                    }
                    OutlinedButton(onClick = { vm.verifyConnection() }, modifier = Modifier.weight(1f), enabled = !state.isVerifying) {
                        Text("Relay check")
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151521))) {
            Column(Modifier.padding(16.dp)) {
                Text("Telegram delivery (optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        state.botUsername == null -> "Relay se bot info nahi mili. Relay pe TELEGRAM_BOT_KEY set hai?"
                        state.telegramLinked -> "✅ Telegram chat linked. Forwards Telegram pe bhi jayengi."
                        else -> "Bot: @${state.botUsername}\n1. Bot kholo → /start dabao\n2. Wapas aake \"Link Telegram\" dabao\nToken/chat ID kahin type nahi karna."
                    },
                    fontSize = 12.sp, color = Color(0xFF94A3B8)
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val user = state.botUsername ?: return@OutlinedButton
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$user?start=${state.pairingId}"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) {}
                        },
                        enabled = !state.botUsername.isNullOrBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Open bot") }
                    Button(
                        onClick = { vm.linkTelegram() },
                        enabled = !state.isVerifying && !state.botUsername.isNullOrBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) { Text("Link Telegram") }
                }
            }
        }

        state.verificationStatus?.let {
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                Text(it, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Relay: ${state.relayUrl}", fontSize = 10.sp, color = Color(0xFF475569))
        Spacer(Modifier.height(4.dp))
        Text("Note: Telegram messages are not E2E encrypted. Receiver gets copies via relay polling — no token needed on either phone.", fontSize = 11.sp, color = Color(0xFF64748B))
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
