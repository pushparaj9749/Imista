package com.imista.forward.sender

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.imista.forward.sender.security.SecureStorage
import com.imista.forward.sender.ui.screens.BlackScreen
import com.imista.forward.sender.ui.screens.MinimalSettingsScreen
import com.imista.forward.sender.ui.screens.SetupScreen
import com.imista.forward.sender.ui.theme.ImistaSenderTheme
import com.imista.forward.sender.viewmodel.SetupViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImistaSenderTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    SenderRootWithPermissions()
                }
            }
        }
    }
}

@Composable
fun SenderRootWithPermissions() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val secure = remember { SecureStorage(context) }
    val setupVm: SetupViewModel = viewModel()

    var showPostNotifDialog by remember { mutableStateOf(false) }
    var showListenerDialog by remember { mutableStateOf(false) }
    var hasChecked by remember { mutableStateOf(false) }

    fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat?.contains(context.packageName) == true
    }
    fun isPostNotifGranted(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val postNotifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // after post notif, check listener
        if (!isNotificationListenerEnabled()) showListenerDialog = true
        setupVm.refresh()
        hasChecked = true
    }

    LaunchedEffect(Unit) {
        // Delay slightly to let UI settle, then request all at once
        kotlinx.coroutines.delay(500)
        if (!isPostNotifGranted()) {
            showPostNotifDialog = true
        } else if (!isNotificationListenerEnabled()) {
            showListenerDialog = true
        } else {
            setupVm.refresh()
            hasChecked = true
        }
    }

    // Post notification popup
    if (showPostNotifDialog) {
        AlertDialog(
            onDismissRequest = { showPostNotifDialog = false; if (!isNotificationListenerEnabled()) showListenerDialog = true else hasChecked = true },
            title = { Text("Notification Permission") },
            text = { Text("Imista ko notification dikhane ki permission do taaki forwarding status bata sake. Next pe Allow dabao.") },
            confirmButton = {
                TextButton(onClick = {
                    showPostNotifDialog = false
                    if (Build.VERSION.SDK_INT >= 33) {
                        postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        if (!isNotificationListenerEnabled()) showListenerDialog = true
                    }
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { showPostNotifDialog = false; if (!isNotificationListenerEnabled()) showListenerDialog = true }) { Text("Skip") }
            }
        )
    }

    // Notification Listener popup - NOT restricted, direct system setting
    if (showListenerDialog) {
        AlertDialog(
            onDismissRequest = { showListenerDialog = false; hasChecked = true; setupVm.refresh() },
            title = { Text("Notification Access chahiye") },
            text = { Text("Ye sabse zaruri hai. Iske bina WhatsApp/SMS forward nahi hoga. Agle screen pe Imista Sender ko ON kar do.\n\nYe Restricted nahi hai — normal System > Notification access me hai.") },
            confirmButton = {
                TextButton(onClick = {
                    showListenerDialog = false
                    hasChecked = true
                    try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showListenerDialog = false; hasChecked = true; setupVm.refresh() }) { Text("Baad me") }
            }
        )
    }

    // Refresh when returning from settings via simple effect
    LaunchedEffect(showListenerDialog) {
        if (!showListenerDialog) setupVm.refresh()
    }

    SenderRoot(secure, setupVm)
}

@Composable
fun SenderRoot(secure: SecureStorage, setupVm: SetupViewModel) {
    val nav = rememberNavController()
    val startDestination = if (secure.onboardingComplete && secure.forwardingActive) "black" else "setup"

    LaunchedEffect(startDestination) {
        setupVm.refresh()
    }

    NavHost(navController = nav, startDestination = startDestination) {
        composable("setup") {
            SetupScreen(
                vm = setupVm,
                onDone = {
                    nav.navigate("black") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }
        composable("black") {
            BlackScreen(
                vm = setupVm,
                onOpenSettings = { nav.navigate("settings") }
            )
        }
        composable("settings") {
            MinimalSettingsScreen(
                vm = setupVm,
                onBack = { nav.popBackStack() },
                onReset = {
                    nav.navigate("setup") {
                        popUpTo("black") { inclusive = true }
                    }
                }
            )
        }
    }
}
