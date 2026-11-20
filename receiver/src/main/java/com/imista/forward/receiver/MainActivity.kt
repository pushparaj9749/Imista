package com.imista.forward.receiver

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.imista.forward.receiver.ui.screens.*
import com.imista.forward.receiver.ui.theme.ImistaReceiverTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImistaReceiverTheme {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0B0B14)) { ReceiverNav() }
            }
        }
    }
}

@Composable
fun ReceiverNav() {
    val nav = rememberNavController()
    var selected by remember { mutableStateOf("home") }
    Scaffold(containerColor = Color(0xFF0B0B14), bottomBar = {
        NavigationBar(containerColor = Color(0xFF151521)) {
            NavigationBarItem(selected = selected=="home", onClick={selected="home"; nav.navigate("home")}, icon={Icon(Icons.Default.Home, null)}, label={Text("Home")})
            NavigationBarItem(selected = selected=="feed", onClick={selected="feed"; nav.navigate("feed")}, icon={Icon(Icons.Default.Inbox, null)}, label={Text("Feed")})
            NavigationBarItem(selected = selected=="connection", onClick={selected="connection"; nav.navigate("connection")}, icon={Icon(Icons.Default.Link, null)}, label={Text("Connect")})
            NavigationBarItem(selected = selected=="about", onClick={selected=="about"; nav.navigate("about")}, icon={Icon(Icons.Default.Info, null)}, label={Text("About")})
        }
    }) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController=nav, startDestination="home") {
                composable("home") { HomeScreen(onNavigate={ nav.navigate(it) }) }
                composable("connection") { ConnectionScreen() }
                composable("feed") { FeedScreen() }
                composable("filters") { FiltersScreen() }
                composable("privacy") { PrivacyReceiverScreen() }
                composable("history") { HistoryReceiverScreen() }
                composable("about") { AboutReceiverScreen() }
                composable("listener") { TelegramListenerScreen() }
            }
        }
    }
}
