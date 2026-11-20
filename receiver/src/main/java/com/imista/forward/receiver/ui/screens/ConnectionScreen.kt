package com.imista.forward.receiver.ui.screens

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
import com.imista.forward.receiver.viewmodel.ConnectionViewModel

@Composable
fun ConnectionScreen(vm: ConnectionViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Connection Setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Use the SAME bot token and Chat ID as Sender. Sender sends to bot, Receiver reads Telegram notifications for that bot. Both phones must be paired.", color=Color(0xFF94A3B8), fontSize=13.sp)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
            Column(Modifier.padding(14.dp)) {
                Text("Architecture Note", fontWeight=FontWeight.SemiBold, fontSize=14.sp)
                Text("Telegram Bot API does NOT push bot-sent messages to another app. Imista Receiver therefore listens for Telegram app notifications (org.telegram.messenger) and reconstructs forwarded messages. This requires Telegram notifications enabled for the bot and optional Notification Listener permission on Receiver. No competing getUpdates consumers.", fontSize=12.sp, color=Color(0xFF94A3B8))
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value=state.botToken, onValueChange={vm.updateToken(it)}, label={Text("Bot Token")}, placeholder={Text("123456:ABC...")}, modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value=state.chatId, onValueChange={vm.updateChat(it)}, label={Text("Telegram Chat ID")}, placeholder={Text("same as Sender")}, modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick={vm.save()}, modifier=Modifier.fillMaxWidth()) { Text("Save") }
        Spacer(Modifier.height(10.dp))
        Button(onClick={vm.test()}, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF10B981)), enabled=!state.loading) {
            if (state.loading) CircularProgressIndicator(modifier=Modifier.size(18.dp), color=Color.White, strokeWidth=2.dp) else Text("Test Connection")
        }
        state.message?.let {
            Spacer(Modifier.height(12.dp))
            Card(colors=CardDefaults.cardColors(containerColor= if(state.success) Color(0xFF064E3B) else Color(0xFF7F1D1D))) { Text(it, modifier=Modifier.padding(12.dp), color=Color.White, fontSize=13.sp) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Steps:\n1. Create bot via @BotFather, share token securely between phones (QR).\n2. Both phones: start private chat with bot (Start).\n3. Enter token & Chat ID on both devices and Test.\n4. On Receiver, enable Notification Access for Telegram listening (optional but recommended).\n5. On Sender, enable Notification Access and select apps.\n6. Send test notification; Receiver should show in Feed.", fontSize=13.sp, color=Color(0xFFE2E8F0))
    }
}
