package com.imista.forward.shared.util

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import com.imista.forward.shared.model.NotificationPayload
import java.util.UUID

object NotificationParser {

    fun parse(sbn: StatusBarNotification, context: Context): NotificationPayload? {
        val pkg = sbn.packageName ?: return null
        // Ignore our own notifications and system foreground notifications to avoid loops
        if (pkg == "com.imista.forward.sender" || pkg == "com.imista.forward.receiver") return null
        if (pkg == "android" && sbn.id == 1) return null // potential system

        val notification = sbn.notification ?: return null
        // Respect ongoing? We'll include but mark; filtering layer decides
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val category = notification.category

        // Skip empty notifications (no title and no text)
        if (title.isNullOrBlank() && text.isNullOrBlank() && bigText.isNullOrBlank()) {
            // Some apps like progress notifications have no useful content; skip
            return null
        }

        val appName = try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkg
        }

        return NotificationPayload(
            id = UUID.randomUUID().toString(),
            packageName = pkg,
            appName = appName,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            category = category,
            timestamp = sbn.postTime,
            isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        )
    }

    fun isSystemOrOngoing(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification ?: return true
        return (n.flags and Notification.FLAG_ONGOING_EVENT) != 0 && sbn.packageName == "android"
    }
}
