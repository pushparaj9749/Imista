package com.imista.forward.receiver

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.imista.forward.receiver.ui.screens.*
import com.imista.forward.receiver.ui.theme.ImistaReceiverTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { com.imista.forward.receiver.service.RelayPollingService.schedulePeriodic(applicationContext) } catch (_: Exception) {}
        setContent {
            ImistaReceiverTheme {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0B0B14)) { ReceiverRootWithPermissions() }
            }
        }
    }
}

@Composable
fun ReceiverRootWithPermissions() {
    val context = LocalContext.current
    var showPostNotifDialog by remember { mutableStateOf(false) }
    var showListenerDialog by remember { mutableStateOf(false) }

    fun isListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat?.contains(context.packageName) == true
    }
    fun isPostNotifGranted(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        if (!isListenerEnabled()) showListenerDialog = true
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600)
        if (!isPostNotifGranted()) showPostNotifDialog = true
        else if (!isListenerEnabled()) showListenerDialog = true
    }

    if (showPostNotifDialog) {
        AlertDialog(
            onDismissRequest = { showPostNotifDialog = false; if (!isListenerEnabled()) showListenerDialog = true },
            title = { Text("Notification Permission") },
            text = { Text("Receiver ko notification dikhane ki permission do. Allow dabao.") },
            confirmButton = { TextButton(onClick = { showPostNotifDialog = false; if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Allow") } },
            dismissButton = { TextButton(onClick = { showPostNotifDialog = false; if (!isListenerEnabled()) showListenerDialog = true }) { Text("Skip") } }
        )
    }
    if (showListenerDialog) {
        AlertDialog(
            onDismissRequest = { showListenerDialog = false },
            title = { Text("Telegram Notifications ON karo") },
            text = { Text("Receiver Telegram ke notification se forwarded message pakadta hai. Agle screen pe Imista Receiver ko ON kar do. Ye restricted setting nahi hai, normal Notification access hai.") },
            confirmButton = {
                TextButton(onClick = {
                    showListenerDialog = false
                    try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                }) { Text("Open Settings") }
            },
            dismissButton = { TextButton(onClick = { showListenerDialog = false }) { Text("Baad me") } }
        )
    }

    // no-op lifecycle refresh for now

    ReceiverNav()
}

@Composable
fun ReceiverNav() {
    val nav = rememberNavController()
    var selected by remember { mutableStateOf("home") }
    Scaffold(containerColor = Color(0xFF0B0B14), bottomBar = {
        NavigationBar(containerColor = Color(0xFF151521)) {
            NavigationBarItem(selected = selected=="home", onClick={selected="home"; nav.navigate("home")}, icon={Icon(Icons.Default.Home, null)}, label={Text("Home")})
            NavigationBarItem(selected = selected=="feed", onClick={selected="feed"; nav.navigate("feed")}, icon={Icon(Icons.Default.Inbox, null)}, label={Text("Feed")})
            NavigationBarItem(selected = selected=="pairing", onClick={selected="pairing"; nav.navigate("pairing")}, icon={Icon(Icons.Default.Link, null)}, label={Text("Pairing")})
            NavigationBarItem(selected = selected=="about", onClick={selected=="about"; nav.navigate("about")}, icon={Icon(Icons.Default.Info, null)}, label={Text("About")})
        }
    }) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController=nav, startDestination="home") {
                composable("home") { HomeScreen(onNavigate={ nav.navigate(it) }) }
                composable("connection") { ConnectionScreen() }
                composable("pairing") { SetupReceiverScreen() }
                composable("feed") { FeedScreen() }
                composable("sender_status") { com.imista.forward.receiver.ui.screens.SenderStatusScreen(onBack = { nav.popBackStack() }) }
                composable("filters") { FiltersScreen() }
                composable("privacy") { PrivacyReceiverScreen() }
                composable("history") { HistoryReceiverScreen() }
                composable("about") { AboutReceiverScreen() }
                composable("listener") { TelegramListenerScreen() }
            }
        }
    }
}
