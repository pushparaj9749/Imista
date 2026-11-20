package com.imista.forward.sender.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imista.forward.sender.data.database.AppDatabase
import com.imista.forward.sender.security.SecureStorage
import com.imista.forward.shared.model.DeviceRole
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardState(
    val isListenerEnabled: Boolean = false,
    val isTelegramConnected: Boolean = false,
    val isDeviceAdminEnabled: Boolean = false,
    val forwardingEnabled: Boolean = true,
    val notificationsToday: Int = 0,
    val pendingCount: Int = 0,
    val lastDelivery: Long? = null,
    val botUsername: String? = null,
    val pairingId: String? = null
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val secure = SecureStorage(app)
    private val context = getApplication<Application>()

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        observeData()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val listenerEnabled = isNotificationListenerEnabled()
            val telegramConfigured = secure.isConfigured()
            val adminEnabled = context.getSharedPreferences("imista_status", 0).getBoolean("device_admin_enabled", false)
            val pending = try { db.pendingDao().getPending().size } catch (_: Exception) { 0 }
            val today = try {
                val since = System.currentTimeMillis() - 24*3600*1000L
                db.historyDao().countSince(since)
            } catch (_: Exception) { 0 }
            _state.update {
                it.copy(
                    isListenerEnabled = listenerEnabled,
                    isTelegramConnected = telegramConfigured,
                    isDeviceAdminEnabled = adminEnabled,
                    pendingCount = pending,
                    notificationsToday = today,
                    lastDelivery = if (secure.lastDelivery != 0L) secure.lastDelivery else null,
                    pairingId = secure.pairingId
                )
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            db.pendingDao().observePendingCount().collect { count ->
                _state.update { it.copy(pendingCount = count) }
            }
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val pkg = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat?.contains(pkg) == true
    }
}
