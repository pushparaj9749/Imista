package com.imista.forward.shared.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Core notification payload exchanged via Telegram.
 * Sender captures from NotificationListenerService, serializes to Telegram,
 * Receiver reconstructs from Telegram notification listener.
 */
@Parcelize
data class NotificationPayload(
    val id: String, // UUID for deduplication
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val category: String?,
    val timestamp: Long, // epoch millis
    val isOngoing: Boolean = false,
    val isSensitiveRedacted: Boolean = false,
    val senderDeviceId: String? = null,
    val pairingId: String? = null
) : Parcelable {
    fun uniqueKey(): String = "$packageName|$title|$text|$timestamp"

    fun displayText(): String? = bigText ?: text

    fun isDuplicateOf(other: NotificationPayload, windowMs: Long = 5000): Boolean {
        if (packageName != other.packageName) return false
        if (title != other.title) return false
        if ((text ?: "") != (other.text ?: "")) return false
        return kotlin.math.abs(timestamp - other.timestamp) < windowMs
    }
}

/**
 * Delivery queue status for sender
 */
enum class DeliveryStatus {
    PENDING, SENDING, SENT, FAILED, RETRYING
}

data class DeliveryRecord(
    val payloadId: String,
    val status: DeliveryStatus,
    val attempts: Int,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class TelegramDeliveryResult(
    val success: Boolean,
    val messageId: Int?,
    val error: String?,
    val retryAfterSeconds: Int? = null
)
