package com.imista.forward.sender.security

import android.content.Context
import android.provider.Settings
import com.imista.forward.sender.BuildConfig
import com.imista.forward.shared.security.CryptoManager
import com.imista.forward.shared.util.Constants
import java.util.UUID

class SecureStorage(context: Context) {
    private val prefs = CryptoManager.getEncryptedPrefs(context)
    private val ctx = context.applicationContext

    // Legacy token fields (kept for migration, but not used in new relay mode)
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

    // New relay mode fields
    var relayUrl: String
        get() = prefs.getString("relay_url", BuildConfig.RELAY_URL) ?: BuildConfig.RELAY_URL
        set(value) { prefs.edit().putString("relay_url", value).apply() }

    var deviceId: String
        get() {
            var id: String? = prefs.getString("device_id", null)
            if (id.isNullOrBlank()) {
                id = try {
                    Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: UUID.randomUUID().toString()
                } catch (e: Exception) { UUID.randomUUID().toString() }
                // Ensure uniqueness with UUID suffix if ANDROID_ID is generic
                if ((id?.length ?: 0) < 10) id = UUID.randomUUID().toString()
                prefs.edit().putString("device_id", id).apply()
            }
            return id ?: UUID.randomUUID().toString()
        }
        set(value) { prefs.edit().putString("device_id", value).apply() }

    var onboardingComplete: Boolean
        get() = prefs.getBoolean("onboarding_complete", false)
        set(value) { prefs.edit().putBoolean("onboarding_complete", value).apply() }

    var forwardingActive: Boolean
        get() = prefs.getBoolean("forwarding_active", false)
        set(value) { prefs.edit().putBoolean("forwarding_active", value).apply() }

    var deviceName: String
        get() = prefs.getString("device_name", android.os.Build.MODEL ?: "Sender Device") ?: "Sender Device"
        set(value) { prefs.edit().putString("device_name", value).apply() }

    /** Lifetime count of successfully forwarded notifications (shown on the black screen). */
    var totalForwarded: Int
        get() = prefs.getInt("total_forwarded", 0)
        set(value) { prefs.edit().putInt("total_forwarded", value).apply() }

    fun bumpForwarded() = totalForwarded.let { totalForwarded = it + 1 }

    /** Set once the user has seen the first black screen; re-opens never show setup again. */
    var hideAllUi: Boolean
        get() = prefs.getBoolean("hide_all_ui", false)
        set(value) { prefs.edit().putBoolean("hide_all_ui", value).apply() }

    fun isConfigured(): Boolean {
        // New mode: pairingId + relayUrl is sufficient; token stored server-side
        if (!pairingId.isNullOrBlank() && relayUrl.isNotBlank()) return true
        // Fallback legacy token mode
        return !botToken.isNullOrBlank() && !chatId.isNullOrBlank()
    }

    fun isPairingConfigured(): Boolean = !pairingId.isNullOrBlank()

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
