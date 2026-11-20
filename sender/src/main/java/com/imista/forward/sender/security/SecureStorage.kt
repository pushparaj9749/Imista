package com.imista.forward.sender.security

import android.content.Context
import com.imista.forward.shared.security.CryptoManager
import com.imista.forward.shared.util.Constants

class SecureStorage(context: Context) {
    private val prefs = CryptoManager.getEncryptedPrefs(context)
    private val app = context.applicationContext

    var botToken: String?
        get() = prefs.getString(Constants.KEY_BOT_TOKEN, null)
        set(value) { prefs.edit().putString(Constants.KEY_BOT_TOKEN, value).apply() }

    var chatId: String?
        get() = prefs.getString(Constants.KEY_CHAT_ID, null)
        set(value) { prefs.edit().putString(Constants.KEY_CHAT_ID, value).apply() }

    var pairingId: String?
        get() = prefs.getString(Constants.KEY_PAIRING_ID, null)
        set(value) { prefs.edit().putString(Constants.KEY_PAIRING_ID, value).apply() }

    var deviceRole: String
        get() = prefs.getString(Constants.KEY_DEVICE_ROLE, "SENDER") ?: "SENDER"
        set(value) { prefs.edit().putString(Constants.KEY_DEVICE_ROLE, value).apply() }

    var forwardingMode: String
        get() = prefs.getString(Constants.KEY_FORWARDING_MODE, "ALL_APPS") ?: "ALL_APPS"
        set(value) { prefs.edit().putString(Constants.KEY_FORWARDING_MODE, value).apply() }

    var hideSensitive: Boolean
        get() = prefs.getBoolean(Constants.KEY_HIDE_SENSITIVE, false)
        set(value) { prefs.edit().putBoolean(Constants.KEY_HIDE_SENSITIVE, value).apply() }

    var maxPreview: Int
        get() = prefs.getInt(Constants.KEY_MAX_PREVIEW, 500)
        set(value) { prefs.edit().putInt(Constants.KEY_MAX_PREVIEW, value).apply() }

    var encryptionEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_ENCRYPTION_ENABLED, false)
        set(value) { prefs.edit().putBoolean(Constants.KEY_ENCRYPTION_ENABLED, value).apply() }

    var updateOffset: Long
        get() = prefs.getLong(Constants.KEY_UPDATE_OFFSET, 0L)
        set(value) { prefs.edit().putLong(Constants.KEY_UPDATE_OFFSET, value).apply() }

    var lastDelivery: Long
        get() = prefs.getLong(Constants.KEY_LAST_DELIVERY, 0L)
        set(value) { prefs.edit().putLong(Constants.KEY_LAST_DELIVERY, value).apply() }

    fun isConfigured(): Boolean = !botToken.isNullOrBlank() && !chatId.isNullOrBlank()

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
