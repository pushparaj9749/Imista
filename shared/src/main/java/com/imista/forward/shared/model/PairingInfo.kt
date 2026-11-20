package com.imista.forward.shared.model

import java.util.UUID

data class PairingInfo(
    val pairingId: String = UUID.randomUUID().toString(),
    val deviceRole: DeviceRole,
    val deviceName: String,
    val deviceId: String, // ANDROID_ID or UUID
    val createdAt: Long = System.currentTimeMillis(),
    val telegramChatId: String? = null,
    val isVerified: Boolean = false
)

enum class DeviceRole {
    SENDER, RECEIVER
}

data class AppFilter(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val isExcluded: Boolean = false
)

enum class ForwardingMode {
    ALL_APPS, SELECTED_ONLY, EXCLUDE_LIST
}

data class PrivacySettings(
    val hideSensitiveContent: Boolean = false,
    val maxPreviewLength: Int = 500,
    val redactPatterns: List<String> = emptyList()
)

data class ConnectionState(
    val telegramConnected: Boolean = false,
    val telegramUsername: String? = null,
    val lastVerifiedAt: Long? = null,
    val lastError: String? = null,
    val notificationListenerEnabled: Boolean = false,
    val deviceAdminEnabled: Boolean = false
)
