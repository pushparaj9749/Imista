package com.imista.forward.sender.service

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import android.util.Log
import androidx.work.*
import com.imista.forward.sender.security.SecureStorage
import com.imista.forward.shared.network.DeviceStatusRequest
import com.imista.forward.shared.network.RelayRepository
import com.imista.forward.shared.network.StatusInfo
import java.util.concurrent.TimeUnit

/**
 * Collects the Sender phone's live state (battery, charging, network, screen, storage,
 * forwarding health) and pushes it to the relay so the Receiver can show it.
 *
 * Runs even when the app UI is not open — the black screen is just a display, the
 * forwarding and this heartbeat continue in the background.
 */
class DeviceStatusWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val secure = SecureStorage(applicationContext)
        val pairingId = secure.pairingId
        if (pairingId.isNullOrBlank()) return Result.success()

        val status = collectStatus(applicationContext, secure)

        // Always keep a local copy so the black screen and settings can read it instantly.
        applicationContext.getSharedPreferences("imista_status", Context.MODE_PRIVATE)
            .edit()
            .putInt("battery_pct", status.batteryPct ?: -1)
            .putBoolean("charging", status.charging == true)
            .putString("network", status.network)
            .putString("forwarded_total", status.forwardedTotal?.toString())
            .putLong("status_updated", System.currentTimeMillis())
            .apply()

        val res = RelayRepository(secure.relayUrl).pushStatus(pairingId, secure.deviceId, status)
        if (res.isFailure) {
            Log.w("ImistaSender", "status push failed: ${res.exceptionOrNull()?.message}")
            return if (runAttemptCount < 3) Result.retry() else Result.success()
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE = "imista_device_status"

        /** Status used by the black screen and settings — no network involved. */
        fun collectStatus(ctx: Context, secure: SecureStorage): StatusInfo {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }

            val batteryIntent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val charging = batteryIntent?.let { i ->
                when (i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                    BatteryManager.BATTERY_STATUS_CHARGING,
                    BatteryManager.BATTERY_STATUS_FULL -> true
                    else -> false
                }
            }

            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            val network = when {
                caps == null -> "offline"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                else -> "other"
            }

            val screenOn = (ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive == true
            val locked = (ctx.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true

            val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val batterySaver = pm?.isPowerSaveMode == true

            val stat = StatFs(ctx.filesDir.absolutePath)
            val storageFreeMb = (stat.availableBytes / (1024 * 1024)).toInt()

            val statusPrefs = ctx.getSharedPreferences("imista_status", Context.MODE_PRIVATE)
            val listener = statusPrefs.getBoolean("listener_connected", false)
            val totalForwarded = secure.totalForwarded

            return StatusInfo(
                batteryPct = pct,
                charging = charging,
                network = network,
                screenOn = screenOn,
                screenLocked = locked,
                batterySaver = batterySaver,
                forwarding = secure.forwardingActive,
                notificationAccess = listener,
                appVersion = Build.VERSION.RELEASE,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                storageFreeMb = storageFreeMb,
                uptimeMinutes = (android.os.SystemClock.elapsedRealtime() / 60000).toInt(),
                forwardedTotal = totalForwarded,
                updatedAt = System.currentTimeMillis()
            )
        }

        /** Pushes a status snapshot immediately (used after onboarding + on UI open). */
        fun enqueueNow(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<DeviceStatusWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag(UNIQUE)
                .build()
            WorkManager.getInstance(ctx).enqueue(req)
        }

        /** Heartbeat: keeps the Receiver's view of this phone fresh. */
        fun schedulePeriodic(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<DeviceStatusWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag(UNIQUE)
                .build()
            WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(UNIQUE)
        }
    }
}
