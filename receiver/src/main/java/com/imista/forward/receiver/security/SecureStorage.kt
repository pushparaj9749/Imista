package com.imista.forward.receiver.security

import android.content.Context
import com.imista.forward.shared.security.CryptoManager
import com.imista.forward.shared.util.Constants

class SecureStorage(context: Context) {
    private val prefs = CryptoManager.getEncryptedPrefs(context)
    var botToken: String?
        get() = prefs.getString(Constants.KEY_BOT_TOKEN, null)
        set(value) { prefs.edit().putString(Constants.KEY_BOT_TOKEN, value).apply() }
    var chatId: String?
        get() = prefs.getString(Constants.KEY_CHAT_ID, null)
        set(v) { prefs.edit().putString(Constants.KEY_CHAT_ID, v).apply() }
    var pairingId: String?
        get() = prefs.getString(Constants.KEY_PAIRING_ID, null)
        set(v) { prefs.edit().putString(Constants.KEY_PAIRING_ID, v).apply() }
    var updateOffset: Long
        get() = prefs.getLong(Constants.KEY_UPDATE_OFFSET, 0L)
        set(v) { prefs.edit().putLong(Constants.KEY_UPDATE_OFFSET, v).apply() }
    var lastReceived: Long
        get() = prefs.getLong("last_received", 0L)
        set(v) { prefs.edit().putLong("last_received", v).apply() }
    fun isConfigured() = !botToken.isNullOrBlank() && !chatId.isNullOrBlank()
    fun clearAll() = prefs.edit().clear().apply()
}
