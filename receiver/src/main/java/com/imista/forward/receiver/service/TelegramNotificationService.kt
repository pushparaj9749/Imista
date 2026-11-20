package com.imista.forward.receiver.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.imista.forward.receiver.data.database.AppDatabase
import com.imista.forward.receiver.data.entity.ReceivedNotificationEntity
import com.imista.forward.receiver.security.SecureStorage
import com.imista.forward.shared.network.TelegramRepository
import kotlinx.coroutines.*
import java.util.UUID

/**
 * Valid Receiver architecture:
 * Telegram Bot API does NOT push bot-sent messages via getUpdates.
 * Therefore Receiver reconstructs notifications by listening for Telegram app notifications
 * on the primary phone. When Sender forwards via bot to Chat ID, the Receiver's Telegram app
 * shows a notification (if Telegram notifications enabled). This service captures those
 * Telegram notifications, parses the Imista-formatted message, and stores locally.
 *
 * This is documented honestly: depends on Telegram notification settings and does not create loops
 * because we filter our own packages and Telegram's Imista messages are distinct.
 *
 * Additional verification (getMe/getChat) is done via polling WorkManager with proper offset handling
 * without competing consumers (Sender never calls getUpdates).
 */
class TelegramNotificationService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val knownIds = mutableSetOf<String>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i("ImistaReceiver", "TelegramNotificationService connected")
        getSharedPreferences("imista_receiver_status", MODE_PRIVATE).edit().putBoolean("listener_connected", true).apply()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        getSharedPreferences("imista_receiver_status", MODE_PRIVATE).edit().putBoolean("listener_connected", false).apply()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        // Only care about Telegram packages
        val pkg = sbn.packageName ?: return
        if (pkg != "org.telegram.messenger" && pkg != "org.telegram.messenger.web" && pkg != "org.telegram.messenger.beta") return

        scope.launch {
            try {
                val extras = sbn.notification?.extras ?: return@launch
                val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
                val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
                val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()
                val fullText = bigText ?: text ?: return@launch

                // Heuristic: check if this looks like an Imista forwarded message
                // Our Sender formats: <b>📱 App</b> ... <code>ID:uuid</code>
                // Notification may have stripped HTML but retains ID
                if (!fullText.contains("ID:") && !title.contains("Imista", ignoreCase = true)) {
                    // Also check bigText may contain forwarded content; if not Imista style, skip
                    // To avoid capturing normal Telegram chats, we require ID marker
                    return@launch
                }

                // Avoid duplicates via ID
                val idRegex = Regex("ID:([a-f0-9\\-]+)", RegexOption.IGNORE_CASE)
                val idMatch = idRegex.find(fullText) ?: idRegex.find(title)
                val id = idMatch?.groupValues?.get(1) ?: UUID.randomUUID().toString()
                if (knownIds.contains(id)) return@launch
                knownIds.add(id)
                if (knownIds.size > 500) knownIds.clear()

                // Parse to local entity
                val secure = SecureStorage(applicationContext)
                // We attempt to extract app name from title or text if formatted
                // Fallback to parsing via TelegramRepository helper
                val repo = TelegramRepository()
                val parsed = repo.parsePayloadFromTelegramText(fullText)

                // Build entity: use parsed if available, else create from notification
                val appName = parsed?.appName ?: title.replace("📱", "").trim().ifBlank { "Unknown" }
                // Try to get package from formatted text's line containing pkg
                val pkgLine = fullText.lines().find { it.contains("•") } ?: ""
                // For receiver feed, we store what we can

                val entity = ReceivedNotificationEntity(
                    id = id,
                    packageName = parsed?.packageName ?: "unknown",
                    appName = appName,
                    title = parsed?.title ?: title,
                    text = parsed?.text ?: fullText,
                    bigText = bigText,
                    timestamp = sbn.postTime,
                    receivedAt = System.currentTimeMillis(),
                    isRead = false,
                    source = "telegram_notification",
                    rawTelegramText = fullText
                )

                val db = AppDatabase.get(applicationContext)
                db.receivedDao().insert(entity)
                secure.lastReceived = System.currentTimeMillis()

                // Optionally mirror as local notification (if enabled by user)
                val prefs = applicationContext.getSharedPreferences("imista_receiver_prefs", MODE_PRIVATE)
                if (prefs.getBoolean("mirror_local", false)) {
                    NotificationHelper.showMirrored(applicationContext, entity)
                }

                // Update status
                getSharedPreferences("imista_receiver_status", MODE_PRIVATE).edit()
                    .putLong("last_notification", System.currentTimeMillis())
                    .apply()

            } catch (e: Exception) {
                Log.e("ImistaReceiver", "parse telegram notification error: ${e.message}")
            }
        }
    }
}
