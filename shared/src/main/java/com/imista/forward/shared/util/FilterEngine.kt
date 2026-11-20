package com.imista.forward.shared.util

import com.imista.forward.shared.model.AppFilter
import com.imista.forward.shared.model.ForwardingMode
import com.imista.forward.shared.model.NotificationPayload

object FilterEngine {

    fun shouldForward(
        payload: NotificationPayload,
        mode: ForwardingMode,
        filters: List<AppFilter>,
        hideSensitive: Boolean = false,
        duplicateCache: MutableSet<String>? = null,
        recentHistory: List<NotificationPayload> = emptyList()
    ): Boolean {
        // Duplicate suppression: check recent history
        if (isDuplicate(payload, recentHistory)) return false

        // Check duplicate cache by unique key
        val key = "${payload.packageName}|${payload.title}|${payload.text}|${payload.timestamp / 5000}"
        if (duplicateCache != null) {
            if (duplicateCache.contains(key)) return false
        }

        // Never forward our own or empty
        if (payload.packageName.isBlank()) return false

        // Apply mode
        return when (mode) {
            ForwardingMode.ALL_APPS -> {
                // Check exclude list
                val excluded = filters.find { it.packageName == payload.packageName }?.isExcluded == true
                !excluded
            }
            ForwardingMode.SELECTED_ONLY -> {
                filters.any { it.packageName == payload.packageName && it.isEnabled }
            }
            ForwardingMode.EXCLUDE_LIST -> {
                val excluded = filters.find { it.packageName == payload.packageName }?.isExcluded == true
                !excluded
            }
        }
    }

    fun isDuplicate(payload: NotificationPayload, history: List<NotificationPayload>, windowMs: Long = 7000): Boolean {
        return history.any { it.isDuplicateOf(payload, windowMs) }
    }

    fun redactIfNeeded(payload: NotificationPayload, hideSensitive: Boolean, maxPreview: Int = 500): NotificationPayload {
        if (!hideSensitive) return payload
        // Simple redaction: keep app name and title, hide text
        return payload.copy(
            text = "[Hidden]",
            bigText = "[Hidden]",
            isSensitiveRedacted = true
        ).let {
            if (maxPreview > 0 && (it.title?.length ?: 0) > maxPreview) {
                it.copy(title = it.title?.take(maxPreview) + "…")
            } else it
        }
    }
}
