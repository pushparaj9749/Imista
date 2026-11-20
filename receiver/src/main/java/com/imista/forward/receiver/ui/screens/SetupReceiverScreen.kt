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
        Text("Sender pe jo 6-digit code dikhega wahi yahan daalo. Koi bot token ya chat ID nahi chahiye — sab relay pe server-side hai.", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
            Column(Modifier.padding(14.dp)) {
                Text("How pairing works", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("1. Sender app kholo → relay 6-digit code deta hai\n2. Wahi code yahan daalo → jud gaye\n3. Sender ka notification → relay store karta hai → Receiver poll karta hai\n4. Telegram bhi chahiye to Connect screen se ek tap me link ho jayega", fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = state.pairingId, onValueChange = { vm.updatePairingId(it) }, label = { Text("6-digit code") }, placeholder = { Text("123456") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { vm.generateNew() }, modifier = Modifier.weight(1f)) { Text("Clear") }
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
        Text("Pairing ke baad Receiver har 15 min me relay poll karta hai (WorkManager) aur screen kholte hi turant. Koi Chat ID/token entry nahi — Telegram delivery relay server-side handle karta hai.", fontSize = 12.sp, color = Color(0xFF94A3B8))
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { vm.clear() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))) { Text("Clear Pairing") }
    }
}
