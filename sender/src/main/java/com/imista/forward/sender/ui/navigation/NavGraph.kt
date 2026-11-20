package com.imista.forward.sender.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object TelegramConfig : Screen("telegram", "Telegram")
    object NotificationAccess : Screen("notification_access", "Notification Access")
    object AppSelection : Screen("app_selection", "Apps")
    object Filtering : Screen("filtering", "Filtering")
    object DeviceAdmin : Screen("device_admin", "Device Admin")
    object History : Screen("history", "History")
    object Privacy : Screen("privacy", "Privacy")
    object About : Screen("about", "About")
    object Pairing : Screen("pairing", "Pairing")
}
