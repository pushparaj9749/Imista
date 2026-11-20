package com.imista.forward.receiver.data.dao

import androidx.room.*
import com.imista.forward.receiver.data.entity.ReceivedNotificationEntity
import com.imista.forward.receiver.data.entity.SettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceivedDao {
    @Query("SELECT * FROM received_notifications ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ReceivedNotificationEntity>>

    @Query("SELECT * FROM received_notifications ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ReceivedNotificationEntity>

    @Query("SELECT * FROM received_notifications WHERE appName LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<ReceivedNotificationEntity>>

    @Query("SELECT * FROM received_notifications WHERE packageName = :pkg ORDER BY timestamp DESC")
    fun byPackage(pkg: String): Flow<List<ReceivedNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReceivedNotificationEntity)

    @Query("UPDATE received_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE received_notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM received_notifications")
    suspend fun clearAll()

    @Query("DELETE FROM received_notifications WHERE receivedAt < :before")
    suspend fun prune(before: Long)

    @Query("SELECT COUNT(*) FROM received_notifications WHERE isRead = 0")
    fun unreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM received_notifications")
    suspend fun totalCount(): Int
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): SettingEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entity: SettingEntity)
}
