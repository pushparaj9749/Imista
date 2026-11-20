package com.imista.forward.receiver.service

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class RelayPollingWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        // Minimal stub - relay polling via secure relay (token server-side)
        // Actual implementation stores via Room after polling relay endpoint
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
    fun enqueueOnce(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<RelayPollingWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx).enqueue(req)
    }
}
