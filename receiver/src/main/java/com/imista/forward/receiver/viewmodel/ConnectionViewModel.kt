package com.imista.forward.receiver.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imista.forward.receiver.security.SecureStorage
import com.imista.forward.receiver.service.PollingService
import com.imista.forward.shared.network.TelegramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ConnState(
    val botToken: String = "",
    val chatId: String = "",
    val loading: Boolean = false,
    val message: String? = null,
    val success: Boolean = false,
    val botUsername: String? = null
)

class ConnectionViewModel(app: Application): AndroidViewModel(app) {
    private val secure = SecureStorage(app)
    private val repo = TelegramRepository()
    private val _state = MutableStateFlow(ConnState(
        botToken = secure.botToken ?: "",
        chatId = secure.chatId ?: ""
    ))
    val state: StateFlow<ConnState> = _state

    fun updateToken(v: String) { _state.value = _state.value.copy(botToken = v.trim()) }
    fun updateChat(v: String) { _state.value = _state.value.copy(chatId = v.trim()) }

    fun save() {
        secure.botToken = _state.value.botToken
        secure.chatId = _state.value.chatId
        if (secure.pairingId == null) secure.pairingId = UUID.randomUUID().toString()
        _state.value = _state.value.copy(message = "Saved", success = true)
    }

    fun test() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = null)
            val s = _state.value
            if (s.botToken.isBlank() || s.chatId.isBlank()) {
                _state.value = _state.value.copy(loading = false, message = "Both fields required")
                return@launch
            }
            val bot = repo.verifyBotToken(s.botToken)
            if (bot.isFailure) {
                _state.value = _state.value.copy(loading = false, message = "Bot failed: ${bot.exceptionOrNull()?.message}")
                return@launch
            }
            val chat = repo.verifyChat(s.botToken, s.chatId)
            if (chat.isSuccess) {
                secure.botToken = s.botToken; secure.chatId = s.chatId
                if (secure.pairingId == null) secure.pairingId = UUID.randomUUID().toString()
                PollingService.enqueueOnce(getApplication())
                _state.value = _state.value.copy(loading = false, message = "✅ Connected to @${bot.getOrNull()?.username} • Chat OK", success = true, botUsername = bot.getOrNull()?.username)
            } else {
                _state.value = _state.value.copy(loading = false, message = "Bot OK but chat invalid: ${chat.exceptionOrNull()?.message}. Ensure you started bot.", success = false, botUsername = bot.getOrNull()?.username)
            }
        }
    }

    fun pairFromQr(qr: String) {
        val info = com.imista.forward.shared.validation.PairingValidator.parseQrPayload(qr)
        if (info != null) {
            secure.pairingId = info.pairingId
            secure.chatId = info.telegramChatId
            _state.value = _state.value.copy(chatId = info.telegramChatId ?: "", message = "QR paired: ${info.pairingId.take(8)}")
        } else {
            _state.value = _state.value.copy(message = "Invalid QR")
        }
    }
}
