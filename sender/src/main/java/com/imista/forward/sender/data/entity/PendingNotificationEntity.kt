package com.imista.forward.sender.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_notifications")
data class PendingNotificationEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val category: String?,
    val timestamp: Long,
    val pairingId: String?,
    val status: String, // DeliveryStatus name
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "delivery_history")
data class DeliveryHistoryEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long,
    val status: String,
    val deliveredAt: Long,
    val error: String?
)

@Entity(tableName = "app_filters")
data class AppFilterEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val isExcluded: Boolean = false
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
