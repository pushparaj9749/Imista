package com.imista.forward.receiver.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imista.forward.receiver.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ReceiverSetupState(
    val pairingId: String = "",
    val relayUrl: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val success: Boolean = false,
    val isPaired: Boolean = false
)

class SetupViewModelReceiver(app: Application) : AndroidViewModel(app) {
    private val secure = SecureStorage(app)
    private val _state = MutableStateFlow(ReceiverSetupState(
        pairingId = secure.pairingId ?: "",
        relayUrl = secure.relayUrl,
        isPaired = secure.isConfigured()
    ))
    val state: StateFlow<ReceiverSetupState> = _state

    fun updatePairingId(id: String) { _state.value = _state.value.copy(pairingId = id.filter { it.isDigit() }.take(6)) }
    fun generateNew() { _state.value = _state.value.copy(pairingId = "") }

    fun saveAndRegister() {
        viewModelScope.launch {
            val code = _state.value.pairingId.trim()
            if (code.length != 6) {
                _state.value = _state.value.copy(message = "6-digit code chahiye", success = false)
                return@launch
            }
            _state.value = _state.value.copy(isLoading = true, message = null)
            val relay = com.imista.forward.shared.network.RelayRepository(secure.relayUrl)
            val res = relay.register(code, secure.deviceId, secure.deviceName, "RECEIVER")
            if (res.isFailure) {
                _state.value = _state.value.copy(isLoading = false, success = false, message = res.exceptionOrNull()?.message ?: "Relay error")
                return@launch
            }
            secure.pairingId = code
            com.imista.forward.receiver.service.RelayPollingService.enqueueOnce(getApplication())
            com.imista.forward.receiver.service.RelayPollingService.schedulePeriodic(getApplication())
            _state.value = _state.value.copy(isLoading = false, message = "✅ Relay se jud gaya", success = true, isPaired = true)
        }
    }

    fun verify() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val code = _state.value.pairingId.trim()
            val relay = com.imista.forward.shared.network.RelayRepository(secure.relayUrl)
            val health = relay.health()
            if (health.isFailure) {
                _state.value = _state.value.copy(isLoading = false, success = false, message = "Relay unreachable: ${health.exceptionOrNull()?.message}")
                return@launch
            }
            val check = relay.getPairing(code)
            _state.value = if (check.isSuccess) {
                _state.value.copy(isLoading = false, message = "✅ Relay connected", success = true, isPaired = true)
            } else {
                _state.value.copy(isLoading = false, success = false, message = "Code relay pe nahi mila — Sender se naya code lo")
            }
        }
    }
    fun clear() { secure.clearAll(); _state.value = ReceiverSetupState() }
}
