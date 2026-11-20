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
    val botUsername: String? = null
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
            val connected = secure.isConfigured()
            val total = try { db.receivedDao().totalCount() } catch (_: Exception) { 0 }
            _state.value = _state.value.copy(
                isListenerEnabled = listener,
                telegramConnected = connected,
                lastReceived = if (secure.lastReceived != 0L) secure.lastReceived else null,
                totalReceived = total
            )
        }
        viewModelScope.launch {
            db.receivedDao().unreadCount().collect { unread ->
                _state.value = _state.value.copy(unread = unread)
            }
        }
    }

    private fun isListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
        return flat?.contains(ctx.packageName) == true
    }
}
