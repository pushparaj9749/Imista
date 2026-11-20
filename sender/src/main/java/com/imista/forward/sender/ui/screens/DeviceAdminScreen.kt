package com.imista.forward.sender.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imista.forward.sender.receiver.ImistaDeviceAdminReceiver

@Composable
fun DeviceAdminScreen() {
    val ctx = LocalContext.current
    var isAdmin by remember { mutableStateOf(isDeviceAdminActive(ctx)) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Device Admin (Optional)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Device Admin allows the Sender app to lock the device where supported. It is entirely optional and not required for notification forwarding.", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Current Status: ${if (isAdmin) "Active ✅" else "Inactive"}", fontWeight = FontWeight.SemiBold, color = if (isAdmin) Color(0xFF10B981) else Color(0xFFEF4444))
                Spacer(Modifier.height(8.dp))
                Text("What does it do?\n• Allows app to request device lock via DevicePolicyManager.lockNow() where permitted.\n• Shows in Settings → Security → Device Administrators.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                Spacer(Modifier.height(8.dp))
                Text("What it does NOT do:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("• Ordinary Device Admin does NOT make the app uninstall-proof. User can deactivate admin first, then uninstall via normal flow.\n• No hidden activation, no uninstall bypass, no persistence against owner wishes.\n• For child-proof protection, use Android Enterprise Device Owner or dedicated parental controls.", fontSize = 13.sp, color = Color(0xFFF59E0B))
            }
        }
        Spacer(Modifier.height(20.dp))
        if (isAdmin) {
            Button(onClick = {
                try {
                    val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    // User must deactivate manually via settings; we can guide
                    ctx.startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                } catch (_: Exception) {}
                isAdmin = isDeviceAdminActive(ctx)
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) { Text("Open Settings to Deactivate") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                try {
                    val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    if (isAdmin) dpm.lockNow()
                } catch (_: Exception) {}
            }, modifier = Modifier.fillMaxWidth()) { Text("Test Lock Device") }
        } else {
            Button(onClick = {
                val comp = ComponentName(ctx, ImistaDeviceAdminReceiver::class.java)
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Imista requests Device Admin only for optional device lock. No data collection.")
                }
                try { ctx.startActivity(intent) } catch (_: Exception) {}
            }, modifier = Modifier.fillMaxWidth()) { Text("Enable Device Admin") }
        }
        Spacer(Modifier.height(12.dp))
        Text("You can use all notification forwarding features without enabling this. Deactivation is always user-controlled via system settings.", fontSize = 12.sp, color = Color(0xFF94A3B8))
    }
}

fun isDeviceAdminActive(ctx: Context): Boolean {
    return try {
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val comp = ComponentName(ctx, ImistaDeviceAdminReceiver::class.java)
        dpm.isAdminActive(comp)
    } catch (_: Exception) { false }
}
