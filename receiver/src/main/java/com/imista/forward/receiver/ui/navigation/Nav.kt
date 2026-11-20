package com.imista.forward.receiver.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Connection : Screen("connection", "Connection")
    object Feed : Screen("feed", "Feed")
    object SenderStatus : Screen("sender_status", "Sender status")
    object Filters : Screen("filters", "Filters")
    object Details : Screen("details", "Details")
    object History : Screen("history", "History")
    object Privacy : Screen("privacy", "Privacy")
    object About : Screen("about", "About")
}
