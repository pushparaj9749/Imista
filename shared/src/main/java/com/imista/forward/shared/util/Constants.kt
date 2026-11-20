package com.imista.forward.shared.util

object Constants {
    const val PREFS_NAME = "imista_prefs"
    const val KEY_BOT_TOKEN = "bot_token"
    const val KEY_CHAT_ID = "chat_id"
    const val KEY_PAIRING_ID = "pairing_id"
    const val KEY_DEVICE_ROLE = "device_role"
    const val KEY_FORWARDING_MODE = "forwarding_mode"
    const val KEY_HIDE_SENSITIVE = "hide_sensitive"
    const val KEY_MAX_PREVIEW = "max_preview"
    const val KEY_ENCRYPTION_ENABLED = "encryption_enabled"
    const val KEY_UPDATE_OFFSET = "update_offset"
    const val KEY_LAST_DELIVERY = "last_delivery"
    const val KEY_NOTIFICATIONS_TODAY = "notifications_today"

    const val DEFAULT_POLL_INTERVAL_MINUTES = 15L
    const val MAX_RETRY_ATTEMPTS = 5
    const val DUPLICATE_WINDOW_MS = 7000L

    val SUPPORTED_APPS = listOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "com.instagram.android",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "com.discord",
        "com.facebook.orca",
        "com.snapchat.android"
    )
}
