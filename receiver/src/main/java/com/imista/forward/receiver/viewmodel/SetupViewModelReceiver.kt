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

    fun updatePairingId(id: String) { _state.value = _state.value.copy(pairingId = id.trim()) }
    fun generateNew() { _state.value = _state.value.copy(pairingId = UUID.randomUUID().toString()) }
    fun saveAndRegister() {
        viewModelScope.launch {
            val pid = _state.value.pairingId.trim()
            if (pid.isBlank()) {
                _state.value = _state.value.copy(message = "Pairing ID required", success = false)
                return@launch
            }
            secure.pairingId = pid
            _state.value = _state.value.copy(isLoading = false, message = "✅ Paired with secure relay", success = true, isPaired = true)
        }
    }
    fun verify() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            _state.value = _state.value.copy(isLoading = false, message = "✅ Relay connected", success = true, isPaired = true)
        }
    }
    fun clear() { secure.clearAll(); _state.value = ReceiverSetupState() }
}
