package com.imista.forward.sender.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imista.forward.sender.security.SecureStorage
import com.imista.forward.shared.network.TelegramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class TelegramState(
    val botToken: String = "",
    val chatId: String = "",
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val isSuccess: Boolean = false,
    val botUsername: String? = null
)

class TelegramViewModel(app: Application) : AndroidViewModel(app) {
    private val secure = SecureStorage(app)
    private val repo = TelegramRepository()

    private val _state = MutableStateFlow(TelegramState(
        botToken = secure.botToken ?: "",
        chatId = secure.chatId ?: ""
    ))
    val state: StateFlow<TelegramState> = _state

    fun updateToken(t: String) { _state.value = _state.value.copy(botToken = t.trim()) }
    fun updateChatId(c: String) { _state.value = _state.value.copy(chatId = c.trim()) }

    fun save() {
        val s = _state.value
        secure.botToken = s.botToken
        secure.chatId = s.chatId
        if (secure.pairingId == null) secure.pairingId = UUID.randomUUID().toString()
        _state.value = _state.value.copy(statusMessage = "Saved locally. Test connection to verify.", isSuccess = true)
    }

    fun testConnection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, statusMessage = null)
            val s = _state.value
            if (s.botToken.isBlank() || s.chatId.isBlank()) {
                _state.value = _state.value.copy(isLoading = false, statusMessage = "Bot token and Chat ID required")
                return@launch
            }
            val result = repo.verifyBotToken(s.botToken)
            if (result.isFailure) {
                _state.value = _state.value.copy(isLoading = false, statusMessage = "Bot verification failed: ${result.exceptionOrNull()?.message}", isSuccess = false)
                return@launch
            }
            val bot = result.getOrNull()
            // Verify chat
            val chatRes = repo.verifyChat(s.botToken, s.chatId)
            if (chatRes.isSuccess) {
                // Try send test message
                val fakePayload = com.imista.forward.shared.model.NotificationPayload(
                    id = UUID.randomUUID().toString(),
                    packageName = "com.imista.forward.sender",
                    appName = "Imista Sender",
                    title = "Test Notification",
                    text = "✅ Telegram connection successful! Notifications will be forwarded here.",
                    bigText = null, subText = null, category = null,
                    timestamp = System.currentTimeMillis()
                )
                val delivery = repo.sendNotification(s.botToken, s.chatId, fakePayload)
                if (delivery.success) {
                    secure.botToken = s.botToken
                    secure.chatId = s.chatId
                    if (secure.pairingId == null) secure.pairingId = UUID.randomUUID().toString()
                    _state.value = _state.value.copy(isLoading = false, statusMessage = "✅ Connected as @${bot?.username} — test message sent!", isSuccess = true, botUsername = bot?.username)
                } else {
                    _state.value = _state.value.copy(isLoading = false, statusMessage = "Chat found but send failed: ${delivery.error}", isSuccess = false)
                }
            } else {
                _state.value = _state.value.copy(isLoading = false, statusMessage = "Bot OK @${bot?.username} but Chat ID invalid: ${chatRes.exceptionOrNull()?.message}. Make sure you started a chat with the bot.", isSuccess = false, botUsername = bot?.username)
            }
        }
    }

    fun generatePairingQr(): String {
        val pid = secure.pairingId ?: UUID.randomUUID().toString().also { secure.pairingId = it }
        val chat = secure.chatId ?: ""
        return com.imista.forward.shared.validation.PairingValidator.createPairingQrCodePayload(pid, chat, "SENDER")
    }
}
