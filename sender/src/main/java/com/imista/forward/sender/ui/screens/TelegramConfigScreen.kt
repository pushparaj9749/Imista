package com.imista.forward.sender.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imista.forward.sender.viewmodel.TelegramViewModel

@Composable
fun TelegramConfigScreen(vm: TelegramViewModel = viewModel(), onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Telegram Bot Setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Create your own bot via @BotFather on Telegram. Never share your token. Stored with Android Keystore.", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
            Column(Modifier.padding(14.dp)) {
                Text("How to get Bot Token & Chat ID", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("1. Open Telegram → search @BotFather → /newbot\n2. Copy token (123456:ABC...)\n3. Start a private chat with your new bot (press Start)\n4. Send any message to the bot\n5. In Imista, paste token and enter your Chat ID (see below)", fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top=6.dp))
                Text("To find Chat ID: message @userinfobot or use getUpdates via browser: https://api.telegram.org/bot<TOKEN>/getUpdates", fontSize = 12.sp, color = Color(0xFF38BDF8), modifier = Modifier.padding(top=6.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = state.botToken, onValueChange = { vm.updateToken(it) }, label = { Text("Bot Token") }, placeholder = { Text("123456789:AAH...") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = state.chatId, onValueChange = { vm.updateChatId(it) }, label = { Text("Telegram Chat ID") }, placeholder = { Text("e.g. 123456789") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.save() }, modifier = Modifier.fillMaxWidth()) { Text("Save Locally") }
        Spacer(Modifier.height(10.dp))
        Button(onClick = { vm.testConnection() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), enabled = !state.isLoading) {
            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            else Text("Test & Send Verification")
        }
        state.statusMessage?.let {
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = if (state.isSuccess) Color(0xFF064E3B) else Color(0xFF7F1D1D))) {
                Text(it, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("⚠️ Limitations: Bot API is not a generic client-to-client push. Receiver obtains messages via Telegram notification reconstruction (see Receiver docs). Bot messages are not Secret Chats (not E2E encrypted).", fontSize = 12.sp, color = Color(0xFFF59E0B))
        Spacer(Modifier.height(8.dp))
        Text("QR Pairing payload (for Receiver):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(vm.generatePairingQr().take(60) + "...", fontSize = 11.sp, color = Color(0xFF94A3B8))
    }
}
