package com.imista.forward.receiver.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imista.forward.receiver.security.SecureStorage
import com.imista.forward.shared.network.RelayRepository
import com.imista.forward.shared.validation.PairingValidator
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

    fun updatePairingId(id: String) {
        _state.value = _state.value.copy(pairingId = id.trim())
    }

    fun generateNew() {
        val id = UUID.randomUUID().toString()
        _state.value = _state.value.copy(pairingId = id)
    }

    fun saveAndRegister() {
        viewModelScope.launch {
            val pid = _state.value.pairingId.trim()
            if (pid.isBlank() || !PairingValidator.isValidPairingId(pid)) {
                _state.value = _state.value.copy(message = "Invalid pairing ID (must be UUID)", success = false)
                return@launch
            }
            _state.value = _state.value.copy(isLoading = true, message = null)
            secure.pairingId = pid
            val relay = RelayRepository(secure.relayUrl)
            val res = relay.register(pid, secure.deviceId, secure.deviceName, "RECEIVER")
            if (res.isSuccess) {
                _state.value = _state.value.copy(isLoading = false, message = "✅ Paired with secure relay. Same ID as Sender required.", success = true, isPaired = true)
                // Trigger initial poll
                com.imista.forward.receiver.service.RelayPollingService.enqueueOnce(getApplication())
            } else {
                _state.value = _state.value.copy(isLoading = false, message = "Register failed: ${res.exceptionOrNull()?.message}", success = false)
            }
        }
    }

    fun verify() {
        viewModelScope.launch {
            val pid = _state.value.pairingId.trim().ifBlank { secure.pairingId } ?: return@launch
            _state.value = _state.value.copy(isLoading = true)
            val relay = RelayRepository(secure.relayUrl)
            val health = relay.health()
            if (health.isFailure) {
                _state.value = _state.value.copy(isLoading = false, message = "Relay unreachable: ${health.exceptionOrNull()?.message}", success = false)
                return@launch
            }
            val pairing = relay.getPairing(pid)
            if (pairing.isSuccess) {
                _state.value = _state.value.copy(isLoading = false, message = "✅ Relay connected. Pairing verified.", success = true, isPaired = true)
            } else {
                _state.value = _state.value.copy(isLoading = false, message = "Verification failed: ${pairing.exceptionOrNull()?.message}", success = false)
            }
        }
    }

    fun clear() {
        secure.clearAll()
        _state.value = ReceiverSetupState()
    }
}
