package com.imista.forward.receiver.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imista.forward.receiver.data.database.AppDatabase
import com.imista.forward.receiver.security.SecureStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeState(
    val isListenerEnabled: Boolean = false,
    val telegramConnected: Boolean = false,
    val lastReceived: Long? = null,
    val totalReceived: Int = 0,
    val unread: Int = 0,
    val botUsername: String? = null,
    val pairingCode: String? = null,
    val relayReachable: Boolean = false,
    val lastError: String? = null,
    val sender: com.imista.forward.shared.network.StatusInfo? = null,
    val senderUpdatedAt: Long? = null,
    val senderOnline: Boolean = false
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val secure = SecureStorage(app)
    private val ctx = getApplication<Application>()
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()
    val recent = db.receivedDao().observeAll()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val listener = isListenerEnabled()
            val status = statusPrefs()
            val connected = status.getBoolean("paired", false)
            val total = try { db.receivedDao().totalCount() } catch (_: Exception) { 0 }
            _state.value = _state.value.copy(
                isListenerEnabled = listener,
                telegramConnected = connected,
                lastReceived = if (secure.lastReceived != 0L) secure.lastReceived else null,
                totalReceived = total,
                pairingCode = secure.pairingId,
                relayReachable = connected,
                lastError = status.getString("last_error", null)
            )
            // Pull from the relay as soon as the screen is opened.
            if (secure.isConfigured()) {
                com.imista.forward.receiver.service.RelayPollingService.enqueueOnce(ctx)
                refreshSenderStatus()
            }
        }
        viewModelScope.launch {
            db.receivedDao().unreadCount().collect { unread ->
                _state.value = _state.value.copy(unread = unread)
            }
        }
    }

    /** Live snapshot of the Sender phone (battery, charging, network, forwarding). */
    fun refreshSenderStatus() {
        val code = secure.pairingId ?: return
        viewModelScope.launch {
            val res = com.imista.forward.shared.network.RelayRepository(secure.relayUrl)
                .getStatus(code, secure.deviceId)
            res.onSuccess { r ->
                val updated = r.updatedAt ?: r.status?.updatedAt
                _state.value = _state.value.copy(
                    sender = r.status,
                    senderUpdatedAt = updated,
                    senderOnline = updated != null && System.currentTimeMillis() - updated < 45 * 60 * 1000
                )
            }
        }
    }

    private fun statusPrefs() =
        ctx.getSharedPreferences("imista_receiver_status", android.content.Context.MODE_PRIVATE)

    private fun isListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
        return flat?.contains(ctx.packageName) == true
    }
}
