package com.imista.forward.receiver.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "received_notifications")
data class ReceivedNotificationEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val timestamp: Long,
    val receivedAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val source: String = "telegram", // telegram_notification
    val rawTelegramText: String? = null
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
