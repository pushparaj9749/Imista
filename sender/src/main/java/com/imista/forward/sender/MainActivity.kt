package com.imista.forward.sender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.imista.forward.sender.ui.screens.*
import com.imista.forward.sender.ui.theme.ImistaSenderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImistaSenderTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0B14)) {
                    SenderNav()
                }
            }
        }
    }
}

@Composable
fun SenderNav() {
    val nav = rememberNavController()
    var selected by remember { mutableStateOf("dashboard") }
    Scaffold(
        containerColor = Color(0xFF0B0B14),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF151521)) {
                NavigationBarItem(selected = selected == "dashboard", onClick = { selected = "dashboard"; nav.navigate("dashboard") }, icon = { Icon(Icons.Default.Dashboard, null) }, label = { Text("Home") })
                NavigationBarItem(selected = selected == "telegram", onClick = { selected = "telegram"; nav.navigate("telegram") }, icon = { Icon(Icons.Default.Send, null) }, label = { Text("Telegram") })
                NavigationBarItem(selected = selected == "history", onClick = { selected = "history"; nav.navigate("history") }, icon = { Icon(Icons.Default.History, null) }, label = { Text("History") })
                NavigationBarItem(selected = selected == "about", onClick = { selected = "about"; nav.navigate("about") }, icon = { Icon(Icons.Default.Info, null) }, label = { Text("About") })
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController = nav, startDestination = "dashboard") {
                composable("dashboard") { DashboardScreen(onNavigate = { nav.navigate(it) }) }
                composable("telegram") { TelegramConfigScreen(onBack = { nav.popBackStack() }) }
                composable("notification_access") { NotificationAccessScreen() }
                composable("app_selection") { AppSelectionScreen() }
                composable("filtering") { PrivacyScreen() }
                composable("device_admin") { DeviceAdminScreen() }
                composable("history") { HistoryScreen() }
                composable("privacy") { PrivacyScreen() }
                composable("about") { AboutScreen() }
                composable("pairing") { TelegramConfigScreen(onBack = { nav.popBackStack() }) }
            }
        }
    }
}
