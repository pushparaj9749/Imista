package com.imista.forward.sender.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imista.forward.sender.security.SecureStorage

@Composable
fun PrivacyScreen() {
    val ctx = LocalContext.current
    val secure = remember { SecureStorage(ctx) }
    var hideSensitive by remember { mutableStateOf(secure.hideSensitive) }
    var encryption by remember { mutableStateOf(secure.encryptionEnabled) }
    var maxPreview by remember { mutableStateOf(secure.maxPreview.toFloat()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Privacy & Security", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
            Column(Modifier.padding(14.dp)) {
                Text("🔐 Credentials stored via Android Keystore-backed EncryptedSharedPreferences. No hardcoded tokens, never logged, never uploaded to GitHub.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                Spacer(Modifier.height(8.dp))
                Text("Telegram Bot API messages are NOT end-to-end encrypted. They are encrypted in transit via HTTPS but visible to Telegram servers. For sensitive content, enable app-level encryption below.", fontSize = 13.sp, color = Color(0xFFF59E0B))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Hide Sensitive Content", fontWeight = FontWeight.Medium); Text("Redact text, keep app & title", fontSize = 12.sp, color = Color(0xFF94A3B8)) }
            Switch(checked = hideSensitive, onCheckedChange = { hideSensitive = it; secure.hideSensitive = it })
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("App-Level Encryption", fontWeight = FontWeight.Medium); Text("AES-GCM with Keystore key", fontSize = 12.sp, color = Color(0xFF94A3B8)) }
            Switch(checked = encryption, onCheckedChange = { encryption = it; secure.encryptionEnabled = it })
        }
        Spacer(Modifier.height(12.dp))
        Text("Max preview length: ${maxPreview.toInt()}", fontWeight = FontWeight.Medium)
        Slider(value = maxPreview, onValueChange = { maxPreview = it; secure.maxPreview = it.toInt() }, valueRange = 50f..1000f)
        Spacer(Modifier.height(16.dp))
        Text("Data Retention", fontWeight = FontWeight.SemiBold)
        Text("Local history auto-prunes after 7 days. You can clear manually in History. Deleting app clears all local records. No analytics SDKs, no ads.", fontSize = 13.sp, color = Color(0xFF94A3B8))
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { secure.clearAll() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))) { Text("Disconnect & Clear Credentials") }
    }
}
