package com.imista.forward.sender.service

import android.content.Context
import androidx.work.*
import com.imista.forward.sender.data.database.AppDatabase
import com.imista.forward.sender.security.SecureStorage
import com.imista.forward.shared.model.NotificationPayload
import com.imista.forward.shared.network.RelayRepository
import com.imista.forward.shared.network.TelegramRepository
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * WorkManager delivery supporting both secure relay (preferred) and legacy direct Telegram.
 * Relay mode keeps TELEGRAM_BOT_KEY server-side, never in APK.
 */
class ForwardWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val payloadId = inputData.getString("payloadId") ?: return Result.failure()
        val context = applicationContext
        val db = AppDatabase.get(context)
        val secure = SecureStorage(context)
        val pending = db.pendingDao().getPending().find { it.id == payloadId }
            ?: db.pendingDao().getRecent(100).find { it.id == payloadId }
            ?: return Result.success()

        if (pending.attempts >= com.imista.forward.shared.util.Constants.MAX_RETRY_ATTEMPTS) {
            db.pendingDao().updateStatus(pending.id, "FAILED", pending.attempts, "Max retries", System.currentTimeMillis())
            db.historyDao().insert(
                com.imista.forward.sender.data.entity.DeliveryHistoryEntity(
                    id = pending.id, packageName = pending.packageName, appName = pending.appName,
                    title = pending.title, text = pending.text, timestamp = pending.timestamp,
                    status = "FAILED", deliveredAt = System.currentTimeMillis(), error = "Max retries"
                )
            )
            return Result.failure()
        }

        db.pendingDao().updateStatus(pending.id, "SENDING", pending.attempts, null, System.currentTimeMillis())

        val payload = NotificationPayload(
            id = pending.id, packageName = pending.packageName, appName = pending.appName,
            title = pending.title, text = pending.text, bigText = pending.bigText, subText = pending.subText,
            category = pending.category, timestamp = pending.timestamp, pairingId = pending.pairingId
        )

        // Prefer relay if pairing configured
        val pairingId = secure.pairingId
        val relayUrl = secure.relayUrl
        val deviceId = secure.deviceId

        val result: com.imista.forward.shared.model.TelegramDeliveryResult? = if (!pairingId.isNullOrBlank() && relayUrl.isNotBlank()) {
            // Relay path
            try {
                val relay = RelayRepository(relayUrl)
                val relayRes = relay.forward(pairingId, deviceId, payload)
                if (relayRes.isSuccess) {
                    com.imista.forward.shared.model.TelegramDeliveryResult(true, null, null)
                } else {
                    val err = relayRes.exceptionOrNull()?.message
                    // Check for rate limit hint
                    if (err?.contains("retry_after", true) == true) {
                        com.imista.forward.shared.model.TelegramDeliveryResult(false, null, err, 30)
                    } else com.imista.forward.shared.model.TelegramDeliveryResult(false, null, err ?: "Relay failed")
                }
            } catch (e: Exception) {
                com.imista.forward.shared.model.TelegramDeliveryResult(false, null, e.message ?: "Relay error")
            }
        } else {
            // Legacy direct Telegram (for migration, requires token/chat)
            val token = secure.botToken
            val chatId = secure.chatId
            if (token.isNullOrBlank() || chatId.isNullOrBlank()) {
                db.pendingDao().updateStatus(pending.id, "FAILED", pending.attempts + 1, "Not configured: pairing or token missing", System.currentTimeMillis())
                return Result.failure()
            }
            val repo = TelegramRepository()
            repo.sendNotification(token, chatId, payload, secure.encryptionEnabled)
        }

        return if (result!!.success) {
            db.pendingDao().updateStatus(pending.id, "SENT", pending.attempts + 1, null, System.currentTimeMillis())
            db.historyDao().insert(
                com.imista.forward.sender.data.entity.DeliveryHistoryEntity(
                    id = pending.id, packageName = pending.packageName, appName = pending.appName,
                    title = pending.title, text = pending.text, timestamp = pending.timestamp,
                    status = "SENT", deliveredAt = System.currentTimeMillis(), error = null
                )
            )
            secure.lastDelivery = System.currentTimeMillis()
            Result.success()
        } else {
            val retryAfter = result.retryAfterSeconds
            if (retryAfter != null && retryAfter > 0) {
                db.pendingDao().updateStatus(pending.id, "RETRYING", pending.attempts + 1, "Rate limited: $retryAfter s", System.currentTimeMillis())
                Result.retry()
            } else {
                val newAttempts = pending.attempts + 1
                db.pendingDao().updateStatus(pending.id, "FAILED", newAttempts, result.error, System.currentTimeMillis())
                if (newAttempts < com.imista.forward.shared.util.Constants.MAX_RETRY_ATTEMPTS) Result.retry()
                else {
                    db.historyDao().insert(
                        com.imista.forward.sender.data.entity.DeliveryHistoryEntity(
                            id = pending.id, packageName = pending.packageName, appName = pending.appName,
                            title = pending.title, text = pending.text, timestamp = pending.timestamp,
                            status = "FAILED", deliveredAt = System.currentTimeMillis(), error = result.error
                        )
                    )
                    Result.failure()
                }
            }
        }
    }
}

object ForwardingService {
    fun enqueue(context: Context, payloadId: String) {
        val data = Data.Builder().putString("payloadId", payloadId).build()
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = OneTimeWorkRequestBuilder<ForwardWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag("imista_forward")
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun enqueueAllPending(context: Context) {
        val request = OneTimeWorkRequestBuilder<RetryAllWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag("imista_retry_all")
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}

class RetryAllWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.get(applicationContext)
        val pendings = db.pendingDao().getPending()
        pendings.forEach { entity ->
            ForwardingService.enqueue(applicationContext, entity.id)
            delay(200)
        }
        val weekAgo = System.currentTimeMillis() - 7*24*3600*1000L
        db.historyDao().prune(weekAgo)
        db.pendingDao().pruneSent(weekAgo)
        return Result.success()
    }
}
