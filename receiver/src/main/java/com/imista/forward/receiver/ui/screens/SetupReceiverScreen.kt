package com.imista.forward.receiver.ui.screens

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
import com.imista.forward.receiver.viewmodel.SetupViewModelReceiver

@Composable
fun SetupReceiverScreen(vm: SetupViewModelReceiver = viewModel()) {
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Receiver Pairing — Secure Relay", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Enter same Pairing ID as Sender. Relay holds Telegram token server-side, auto-configured via GitHub Secret. No manual token entry.", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
            Column(Modifier.padding(14.dp)) {
                Text("How pairing works", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("1. Sender generates Pairing ID and registers with relay (relay has TELEGRAM_BOT_KEY)\n2. Copy same ID to Receiver and Register\n3. Sender forwards → relay stores + forwards to Telegram → Receiver polls relay (no competing getUpdates consumers)", fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = state.pairingId, onValueChange = { vm.updatePairingId(it) }, label = { Text("Pairing ID") }, placeholder = { Text("Paste Sender's pairing ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { vm.generateNew() }, modifier = Modifier.weight(1f)) { Text("Generate") }
            Button(onClick = { vm.saveAndRegister() }, modifier = Modifier.weight(1f), enabled = !state.isLoading) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp) else Text("Register")
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = { vm.verify() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), enabled = !state.isLoading) {
            Text("Verify Relay Connection")
        }
        state.message?.let {
            Spacer(Modifier.height(10.dp))
            Card(colors = CardDefaults.cardColors(containerColor = if (state.success) Color(0xFF064E3B) else Color(0xFF7F1D1D))) {
                Text(it, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("After pairing, Receiver polls relay every 15 min (WorkManager) + on demand. No need to enter Chat ID — relay auto-handles Telegram delivery via server token. Both apps use same infrastructure.", fontSize = 12.sp, color = Color(0xFF94A3B8))
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { vm.clear() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))) { Text("Clear Pairing") }
    }
}
