package com.imista.forward.receiver.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imista.forward.receiver.viewmodel.ConnectionViewModel

/**
 * Receiver pairing screen.
 *
 * The user types only the 6-digit code shown on the Sender. The Telegram bot
 * token and chat id stay on the relay, so nothing else has to be entered here.
 */
@Composable
fun ConnectionScreen(vm: ConnectionViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Connect Receiver", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Sender pe jo 6-digit code dikhega wahi yahan daalo. Bas. Koi bot token ya chat ID nahi chahiye — wo relay pe server-side hai.",
            color = Color(0xFF94A3B8), fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
            Column(Modifier.padding(14.dp)) {
                Text("Kaise kaam karta hai", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "1. Sender app khulta hai → relay 6-digit code deta hai\n" +
                    "2. Wahi code yahan daalo → Dono jud gaye\n" +
                    "3. Sender ka notification → relay → yahan Feed me\n" +
                    "4. (Optional) Telegram pe bhi chahiye to neeche wala button dabao",
                    fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = state.code,
            onValueChange = { vm.updateCode(it) },
            label = { Text("6-digit pairing code") },
            placeholder = { Text("123456") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { vm.join() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loading && state.code.length == 6
        ) {
            if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            else Text(if (state.paired) "Re-connect" else "Connect")
        }

        state.message?.let {
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = if (state.success) Color(0xFF064E3B) else Color(0xFF7F1D1D))) {
                Text(it, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151521))) {
            Column(Modifier.padding(14.dp)) {
                Text("Telegram (optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        !state.telegramAvailable -> "Relay pe TELEGRAM_BOT_KEY set nahi hai. Messages yahan Feed me aate rahenge; Telegram chahiye to relay env me token daalna hoga."
                        state.telegramLinked -> "✅ Telegram chat linked — forwards Telegram pe bhi aayengi."
                        else -> "@${state.botUsername} ko kholo → /start dabao → wapas aake \"Link Telegram\". Token kahin type nahi karna."
                    },
                    fontSize = 12.sp, color = Color(0xFF94A3B8)
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val user = state.botUsername ?: return@OutlinedButton
                            val code = state.code.ifBlank { state.pairingCode ?: "" }
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$user?start=$code"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) {}
                        },
                        enabled = state.telegramAvailable && !state.botUsername.isNullOrBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Open bot") }
                    Button(
                        onClick = { vm.linkTelegram() },
                        enabled = state.telegramAvailable && !state.loading,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) { Text("Link Telegram") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.refreshNow() }, modifier = Modifier.weight(1f), enabled = !state.loading) {
                Text("Fetch now")
            }
            OutlinedButton(
                onClick = { vm.unlink() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
            ) { Text("Unpair") }
        }

        Spacer(Modifier.height(20.dp))
        Text("Relay: ${state.relayUrl}", fontSize = 10.sp, color = Color(0xFF475569))
        Text("Device: ${state.deviceId.take(12)}…", fontSize = 10.sp, color = Color(0xFF475569))
    }
}
