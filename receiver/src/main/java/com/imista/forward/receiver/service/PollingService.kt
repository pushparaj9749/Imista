package com.imista.forward.receiver.service

import android.content.Context
import androidx.work.*
import com.imista.forward.receiver.security.SecureStorage
import com.imista.forward.shared.network.TelegramRepository
import java.util.concurrent.TimeUnit

/**
 * PollingService validates Telegram connection and handles pairing commands.
 * It does NOT attempt to fetch forwarded notifications via getUpdates, because Bot API
 * does not deliver bot-sent messages as updates. This is documented.
 * Instead it verifies bot and chat health and handles user commands sent to bot.
 * Polling respects single-consumer semantics: only Receiver polls, Sender never does.
 */
class PollingWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val secure = SecureStorage(applicationContext)
        val token = secure.botToken ?: return Result.success()
        val chatId = secure.chatId ?: return Result.success()
        val repo = TelegramRepository()
        // Verify bot still valid
        val botRes = repo.verifyBotToken(token)
        if (botRes.isFailure) {
            applicationContext.getSharedPreferences("imista_receiver_status", Context.MODE_PRIVATE).edit()
                .putString("last_error", botRes.exceptionOrNull()?.message).apply()
            return Result.retry()
        }
        // Fetch updates to handle pairing commands (user messaging bot directly)
        val offset = secure.updateOffset.takeIf { it != 0L }
        val updatesRes = repo.getUpdates(token, offset, limit = 10, timeout = 0)
        if (updatesRes.isSuccess) {
            val updates = updatesRes.getOrNull() ?: emptyList()
            if (updates.isNotEmpty()) {
                var maxId = offset ?: 0
                updates.forEach { upd ->
                    maxId = maxOf(maxId, upd.updateId)
                    val msg = upd.message ?: return@forEach
                    // If message is from our paired chat and contains pairing command, handle
                    if (msg.chat.id.toString() == chatId && msg.text?.startsWith("/pair") == true) {
                        // Pairing verified
                        applicationContext.getSharedPreferences("imista_receiver_status", Context.MODE_PRIVATE).edit()
                            .putBoolean("pairing_verified", true).apply()
                    }
                }
                secure.updateOffset = maxId + 1
            }
            applicationContext.getSharedPreferences("imista_receiver_status", Context.MODE_PRIVATE).edit()
                .putLong("last_poll", System.currentTimeMillis()).putString("last_error", null).apply()
        }
        return Result.success()
    }
}

object PollingService {
    fun schedulePeriodic(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<PollingWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag("imista_poll")
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("imista_poll", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
    fun enqueueOnce(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<PollingWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx).enqueue(req)
    }
}
