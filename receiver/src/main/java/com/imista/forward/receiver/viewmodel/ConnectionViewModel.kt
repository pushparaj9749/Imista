package com.imista.forward.receiver.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imista.forward.receiver.security.SecureStorage
import com.imista.forward.receiver.service.RelayPollingService
import com.imista.forward.shared.network.RelayRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Receiver connection state. The phone only ever needs the relay URL and the
 * 6-digit pairing code — the Telegram bot token lives on the relay.
 */
data class ConnState(
    val code: String = "",
    val pairingCode: String = "",
    val relayUrl: String = "",
    val deviceId: String = "",
    val botUsername: String? = null,
    val telegramAvailable: Boolean = false,
    val telegramLinked: Boolean = false,
    val paired: Boolean = false,
    val loading: Boolean = false,
    val message: String? = null,
    val success: Boolean = false
)

class ConnectionViewModel(app: Application) : AndroidViewModel(app) {
    private val secure = SecureStorage(app)
    private val _state = MutableStateFlow(
        ConnState(
            code = secure.pairingId ?: "",
            pairingCode = secure.pairingId ?: "",
            relayUrl = secure.relayUrl,
            deviceId = secure.deviceId,
            paired = secure.isConfigured()
        )
    )
    val state: StateFlow<ConnState> = _state

    init {
        viewModelScope.launch {
            val relay = RelayRepository(secure.relayUrl)
            relay.botInfo().onSuccess { info ->
                _state.value = _state.value.copy(
                    botUsername = info.username,
                    telegramAvailable = info.telegramConfigured == true && !info.username.isNullOrBlank()
                )
            }
        }
    }

    fun updateCode(v: String) {
        _state.value = _state.value.copy(code = v.filter { it.isDigit() }.take(6))
    }

    /** Registers the receiver with the relay using the code shown on the Sender. */
    fun join() {
        viewModelScope.launch {
            val s = _state.value
            if (s.code.length != 6) {
                _state.value = s.copy(message = "6-digit code daalo (Sender screen pe dikhega)", success = false)
                return@launch
            }
            _state.value = s.copy(loading = true, message = null)
            val relay = RelayRepository(secure.relayUrl)
            val res = relay.register(
                code = s.code,
                deviceId = secure.deviceId,
                deviceName = secure.deviceName,
                role = "RECEIVER"
            )
            if (res.isFailure) {
                _state.value = _state.value.copy(
                    loading = false,
                    success = false,
                    message = res.exceptionOrNull()?.message ?: "Relay tak nahi pahuncha"
                )
                return@launch
            }
            val pairing = res.getOrNull()?.pairing
            secure.pairingId = s.code
            // First poll immediately so old messages are not lost.
            RelayPollingService.enqueueOnce(getApplication())
            RelayPollingService.schedulePeriodic(getApplication())
            _state.value = _state.value.copy(
                loading = false,
                success = true,
                paired = true,
                pairingCode = s.code,
                telegramLinked = pairing?.telegramLinked == true,
                botUsername = pairing?.botUsername ?: _state.value.botUsername,
                message = "✅ Receiver paired. Ab messages apne aap aane lagenge."
            )
        }
    }

    /**
     * Telegram auto-link: user taps the bot deep link, relay spots the /start
     * message and stores the chat id. No token or chat id typed on the phone.
     */
    fun linkTelegram() {
        viewModelScope.launch {
            val code = _state.value.code.ifBlank { secure.pairingId ?: "" }
            if (code.isBlank()) {
                _state.value = _state.value.copy(message = "Pehle pair karo", success = false)
                return@launch
            }
            _state.value = _state.value.copy(loading = true, message = "Telegram check ho raha hai…")
            val relay = RelayRepository(secure.relayUrl)
            repeat(6) { attempt ->
                val res = relay.link(code)
                val body = res.getOrNull()
                if (res.isSuccess && body?.linked == true) {
                    _state.value = _state.value.copy(
                        loading = false,
                        success = true,
                        telegramLinked = true,
                        message = "✅ Telegram jud gaya. Notifications Telegram pe bhi aayengi."
                    )
                    return@launch
                }
                if (attempt < 5) delay(4000)
            }
            _state.value = _state.value.copy(
                loading = false,
                success = false,
                message = "Telegram link nahi hua. Pehle bot ko /start karo, phir try karo."
            )
        }
    }

    /** Manual pull right now (also useful to verify everything works). */
    fun refreshNow() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = null)
            RelayPollingService.enqueueOnce(getApplication())
            delay(1200)
            _state.value = _state.value.copy(
                loading = false,
                paired = secure.isConfigured(),
                message = "Relay se latest messages fetch kiye"
            )
        }
    }

    fun unlink() {
        secure.pairingId = null
        _state.value = _state.value.copy(paired = false, telegramLinked = false, code = "", message = "Pairing hata di")
    }
}
