package com.imista.forward.receiver.service

import android.content.Context
import androidx.work.*
import com.imista.forward.receiver.data.database.AppDatabase
import com.imista.forward.receiver.data.entity.ReceivedNotificationEntity
import com.imista.forward.receiver.security.SecureStorage
import com.imista.forward.shared.network.RelayRepository
import java.util.concurrent.TimeUnit

/**
 * Pulls new notifications from the Imista relay.
 *
 * This is the primary delivery path: the relay stores every forwarded notification
 * and the Receiver simply asks "anything after my cursor?". No Telegram app, no bot
 * token and no chat id are needed on the phone for this to work.
 */
class RelayPollingWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val secure = SecureStorage(applicationContext)
        val pairingId = secure.pairingId
        val relayUrl = secure.relayUrl
        if (pairingId.isNullOrBlank() || relayUrl.isBlank()) {
            status().edit().putString("last_error", "Not paired yet").putBoolean("paired", false).apply()
            return Result.success()
        }

        val db = AppDatabase.get(applicationContext)
        val relay = RelayRepository(relayUrl)
        val since = secure.relaySince

        val res = relay.poll(pairingId, secure.deviceId, since, limit = 50)
        if (res.isFailure) {
            val msg = res.exceptionOrNull()?.message ?: "Relay unreachable"
            status().edit().putString("last_error", msg).putBoolean("paired", false).apply()
            // Retry on transient failures so nothing is lost.
            return if (runAttemptCount < 3) Result.retry() else Result.success()
        }

        val messages = res.getOrNull()?.messages.orEmpty()
        var newest = since
        var inserted = 0
        messages.forEach { m ->
            val payload = m.raw
            val id = m.id
            val alreadyKnown = try {
                db.receivedDao().exists(id)
            } catch (_: Exception) { false }
            if (!alreadyKnown) {
                db.receivedDao().insert(
                    ReceivedNotificationEntity(
                        id = id,
                        packageName = m.package_name ?: payload?.packageName ?: "unknown",
                        appName = m.app_name ?: payload?.appName ?: "Unknown",
                        title = m.title ?: payload?.title,
                        text = m.text_content ?: payload?.text,
                        bigText = m.big_text ?: payload?.bigText,
                        timestamp = m.timestamp ?: payload?.timestamp ?: System.currentTimeMillis(),
                        receivedAt = System.currentTimeMillis(),
                        isRead = false,
                        source = "relay"
                    )
                )
                inserted++
                if (prefs().getBoolean("mirror_local", false)) {
                    NotificationHelper.showMirrored(
                        applicationContext,
                        ReceivedNotificationEntity(
                            id = id,
                            packageName = m.package_name ?: "unknown",
                            appName = m.app_name ?: "Unknown",
                            title = m.title,
                            text = m.text_content,
                            bigText = m.big_text,
                            timestamp = m.timestamp ?: System.currentTimeMillis()
                        )
                    )
                }
            }
            val cursor = m.created_at ?: m.timestamp ?: 0L
            if (cursor > newest) newest = cursor
        }

        if (newest > since) secure.relaySince = newest
        if (inserted > 0) secure.lastReceived = System.currentTimeMillis()

        status().edit()
            .putBoolean("paired", true)
            .putLong("last_poll", System.currentTimeMillis())
            .putInt("last_fetch_count", inserted)
            .putString("last_error", null)
            .apply()

        return Result.success()
    }

    private fun status() =
        applicationContext.getSharedPreferences("imista_receiver_status", Context.MODE_PRIVATE)

    private fun prefs() =
        applicationContext.getSharedPreferences("imista_receiver_prefs", Context.MODE_PRIVATE)
}

object RelayPollingService {
    private const val UNIQUE = "imista_relay_poll"

    fun schedulePeriodic(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<RelayPollingWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(UNIQUE)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun enqueueOnce(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<RelayPollingWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(UNIQUE)
            .build()
        WorkManager.getInstance(ctx).enqueue(req)
    }
}
