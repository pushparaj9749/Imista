package com.imista.forward.receiver.service

import android.content.Context
import androidx.work.*
import com.imista.forward.receiver.data.database.AppDatabase
import com.imista.forward.receiver.data.entity.ReceivedNotificationEntity
import com.imista.forward.receiver.security.SecureStorage
import com.imista.forward.shared.network.RelayRepository
import java.util.concurrent.TimeUnit

/**
 * Relay polling replaces direct Telegram getUpdates.
 * Single source of truth is relay (which holds token server-side), so no competing consumers.
 * Polls relay for new messages since last cursor, stores locally, updates UI feed.
 */
class RelayPollingWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val secure = SecureStorage(context)
        val pairingId = secure.pairingId ?: return Result.success()
        val deviceId = secure.deviceId
        val relayUrl = secure.relayUrl
        if (pairingId.isBlank() || relayUrl.isBlank()) return Result.success()

        val repo = RelayRepository(relayUrl)
        val since = secure.relaySince
        val res = repo.poll(pairingId, deviceId, since, 50)
        if (res.isFailure) {
            context.getSharedPreferences("imista_receiver_status", Context.MODE_PRIVATE).edit()
                .putString("last_error", res.exceptionOrNull()?.message).apply()
            return Result.retry()
        }
        val poll = res.getOrNull() ?: return Result.success()
        if (poll.messages.isNullOrEmpty()) {
            context.getSharedPreferences("imista_receiver_status", Context.MODE_PRIVATE).edit()
                .putLong("last_poll", System.currentTimeMillis()).apply()
            return Result.success()
        }

        val db = AppDatabase.get(context)
        var maxCreated = since
        for (msg in poll.messages) {
            // Relay message may contain raw payload or flat fields
            val payload = msg.raw
            val appName = msg.app_name ?: payload?.appName ?: "Unknown"
            val pkg = msg.package_name ?: payload?.packageName ?: "unknown"
            val title = msg.title ?: payload?.title
            val text = msg.text_content ?: payload?.text ?: payload?.bigText ?: ""
            val bigText = msg.big_text ?: payload?.bigText
            val ts = msg.timestamp ?: payload?.timestamp ?: System.currentTimeMillis()
            val createdAt = msg.created_at ?: System.currentTimeMillis()
            maxCreated = maxOf(maxCreated, createdAt)

            val entity = ReceivedNotificationEntity(
                id = msg.id ?: payload?.id ?: java.util.UUID.randomUUID().toString(),
                packageName = pkg,
                appName = appName,
                title = title,
                text = text,
                bigText = bigText,
                timestamp = ts,
                receivedAt = createdAt,
                isRead = false,
                source = "relay",
                rawTelegramText = null
            )
            try { db.receivedDao().insert(entity) } catch (_: Exception) {}
        }
        secure.relaySince = maxCreated
        secure.lastReceived = System.currentTimeMillis()
        context.getSharedPreferences("imista_receiver_status", Context.MODE_PRIVATE).edit()
            .putLong("last_poll", System.currentTimeMillis())
            .putLong("last_message_count", poll.messages.size.toLong())
            .putString("last_error", null)
            .apply()

        // Optional: trigger local notification mirror if enabled
        val prefs = context.getSharedPreferences("imista_receiver_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("mirror_local", false) && poll.messages.isNotEmpty()) {
            // Mirror last message as local notification (avoid loops via pkg filter already in helper)
            try {
                val last = poll.messages.last()
                val mirrorEntity = ReceivedNotificationEntity(
                    id = last.id, packageName = last.package_name ?: "relay", appName = last.app_name ?: "Imista",
                    title = last.title, text = last.text_content, bigText = last.big_text,
                    timestamp = last.timestamp ?: System.currentTimeMillis()
                )
                NotificationHelper.showMirrored(context, mirrorEntity)
            } catch (_: Exception) {}
        }

        return Result.success()
    }
}

object RelayPollingService {
    fun schedulePeriodic(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<RelayPollingWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag("imista_relay_poll")
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("imista_relay_poll", ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun scheduleFrequent(ctx: Context) {
        // For testing, also schedule 1-min expedited poll
        val req = PeriodicWorkRequestBuilder<RelayPollingWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx).enqueue(req)
    }

    fun enqueueOnce(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<RelayPollingWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag("imista_relay_once")
            .build()
        WorkManager.getInstance(ctx).enqueue(req)
    }
}
