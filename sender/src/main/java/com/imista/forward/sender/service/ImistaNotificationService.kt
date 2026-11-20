package com.imista.forward.sender.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.imista.forward.sender.data.database.AppDatabase
import com.imista.forward.sender.data.entity.PendingNotificationEntity
import com.imista.forward.sender.security.SecureStorage
import com.imista.forward.shared.model.ForwardingMode
import com.imista.forward.shared.util.Constants
import com.imista.forward.shared.util.FilterEngine
import com.imista.forward.shared.util.NotificationParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class ImistaNotificationService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val duplicateCache = mutableSetOf<String>()
    private var lastCleanup = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()
        Log.i("ImistaSender", "NotificationListenerService created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i("ImistaSender", "NotificationListener connected")
        // Notify UI via broadcast or shared pref flag
        getSharedPreferences("imista_status", MODE_PRIVATE).edit().putBoolean("listener_connected", true).apply()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w("ImistaSender", "NotificationListener disconnected")
        getSharedPreferences("imista_status", MODE_PRIVATE).edit().putBoolean("listener_connected", false).apply()
        // Request rebind
        try { requestRebind(android.content.ComponentName(this, ImistaNotificationService::class.java)) } catch (_: Exception) {}
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        scope.launch {
            try {
                handleNotification(sbn)
            } catch (e: Exception) {
                Log.e("ImistaSender", "handleNotification error: ${e.message}")
            }
        }
    }

    private suspend fun handleNotification(sbn: StatusBarNotification) {
        val context = applicationContext
        val secure = SecureStorage(context)
        // Black-screen mode: forwardingActive controls actual forwarding, UI is just black screen
        if (!secure.forwardingActive) return
        if (!secure.isConfigured()) return
        val db = AppDatabase.get(context)

        // Parse
        val payload = NotificationParser.parse(sbn, context) ?: return

        // Skip ongoing if needed, but allow filtering to decide
        if (payload.isOngoing && payload.packageName == "android") return

        // Get filtering config
        val mode = try { ForwardingMode.valueOf(secure.forwardingMode) } catch (_: Exception) { ForwardingMode.ALL_APPS }
        val filters = db.filterDao().getAll().map {
            com.imista.forward.shared.model.AppFilter(it.packageName, it.appName, it.isEnabled, it.isExcluded)
        }
        val hideSensitive = secure.hideSensitive

        // Recent history for duplicate suppression (in-memory + DB)
        val history = db.pendingDao().getRecent(50).map {
            com.imista.forward.shared.model.NotificationPayload(
                id = it.id, packageName = it.packageName, appName = it.appName,
                title = it.title, text = it.text, bigText = it.bigText, subText = it.subText,
                category = it.category, timestamp = it.timestamp
            )
        }

        if (!FilterEngine.shouldForward(payload, mode, filters, hideSensitive, duplicateCache, history)) {
            return
        }

        // Apply redaction
        val finalPayload = FilterEngine.redactIfNeeded(payload, hideSensitive, secure.maxPreview)

        // Deduplicate via cache key
        val dupKey = "${finalPayload.packageName}|${finalPayload.title}|${finalPayload.text}|${finalPayload.timestamp/5000}"
        if (duplicateCache.contains(dupKey)) return
        duplicateCache.add(dupKey)
        // Cleanup cache periodically
        if (System.currentTimeMillis() - lastCleanup > 60_000) {
            if (duplicateCache.size > 200) duplicateCache.clear()
            lastCleanup = System.currentTimeMillis()
        }

        // Create pending entity
        val entity = PendingNotificationEntity(
            id = finalPayload.id,
            packageName = finalPayload.packageName,
            appName = finalPayload.appName,
            title = finalPayload.title,
            text = finalPayload.text,
            bigText = finalPayload.bigText,
            subText = finalPayload.subText,
            category = finalPayload.category,
            timestamp = finalPayload.timestamp,
            pairingId = secure.pairingId,
            status = "PENDING",
            attempts = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.pendingDao().insert(entity)

        // Enqueue delivery via WorkManager
        val pairing = secure.pairingId
        // Use ForwardingService.enqueue
        ForwardingService.enqueue(context, finalPayload.id)

        // Also record status for UI
        getSharedPreferences("imista_status", MODE_PRIVATE).edit()
            .putLong("last_seen", System.currentTimeMillis())
            .putInt("pending_count", db.pendingDao().getPending().size)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
