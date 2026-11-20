package com.imista.forward.receiver.security

import android.content.Context
import android.provider.Settings
import com.imista.forward.receiver.BuildConfig
import com.imista.forward.shared.security.CryptoManager
import com.imista.forward.shared.util.Constants
import java.util.UUID

class SecureStorage(context: Context) {
    private val prefs = CryptoManager.getEncryptedPrefs(context)
    private val ctx = context.applicationContext

    // Legacy token fields (migrated, not used in relay mode but kept)
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

    var relayUrl: String
        get() = prefs.getString("relay_url", BuildConfig.RELAY_URL) ?: BuildConfig.RELAY_URL
        set(v) { prefs.edit().putString("relay_url", v).apply() }

    var deviceId: String
        get() {
            var id: String? = prefs.getString("device_id", null)
            if (id.isNullOrBlank()) {
                id = try {
                    Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: UUID.randomUUID().toString()
                } catch (e: Exception) { UUID.randomUUID().toString() }
                if ((id?.length ?: 0) < 10) id = UUID.randomUUID().toString()
                prefs.edit().putString("device_id", id).apply()
            }
            return id ?: UUID.randomUUID().toString()
        }
        set(v) { prefs.edit().putString("device_id", v).apply() }

    var deviceName: String
        get() = prefs.getString("device_name", android.os.Build.MODEL ?: "Receiver Device") ?: "Receiver Device"
        set(v) { prefs.edit().putString("device_name", v).apply() }

    // For receiver relay polling cursor
    var relaySince: Long
        get() = prefs.getLong("relay_since", 0L)
        set(v) { prefs.edit().putLong("relay_since", v).apply() }

    fun isConfigured() = !pairingId.isNullOrBlank() && relayUrl.isNotBlank()
    fun isLegacyConfigured() = !botToken.isNullOrBlank() && !chatId.isNullOrBlank()

    fun clearAll() = prefs.edit().clear().apply()
}
