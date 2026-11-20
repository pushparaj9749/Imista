package com.imista.forward.sender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                    SenderRoot()
                }
            }
        }
    }
}

@Composable
fun SenderRoot() {
    val nav = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val secure = remember { SecureStorage(context) }
    val startDestination = if (secure.onboardingComplete && secure.forwardingActive) "black" else "setup"

    // Keep viewmodel alive across nav
    val setupVm: SetupViewModel = viewModel()

    LaunchedEffect(startDestination) {
        // refresh on launch
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
