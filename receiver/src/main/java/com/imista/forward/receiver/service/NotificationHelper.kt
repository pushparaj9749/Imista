package com.imista.forward.receiver.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.imista.forward.receiver.data.entity.ReceivedNotificationEntity

object NotificationHelper {
    private const val CHANNEL_ID = "imista_mirror"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Imista Mirrored", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Local mirror of forwarded notifications"
            }
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    fun showMirrored(ctx: Context, entity: ReceivedNotificationEntity) {
        ensureChannel(ctx)
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${entity.appName}: ${entity.title ?: ""}")
            .setContentText(entity.text ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(entity.text ?: ""))
            .setAutoCancel(true)
            .setGroup("imista_forward")
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(entity.id.hashCode(), notif)
    }
}
