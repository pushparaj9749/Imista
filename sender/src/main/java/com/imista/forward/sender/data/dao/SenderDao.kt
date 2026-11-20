package com.imista.forward.sender.data.dao

import androidx.room.*
import com.imista.forward.sender.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingNotificationDao {
    @Query("SELECT * FROM pending_notifications ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PendingNotificationEntity>>

    @Query("SELECT * FROM pending_notifications WHERE status IN ('PENDING','FAILED','RETRYING') ORDER BY createdAt ASC")
    suspend fun getPending(): List<PendingNotificationEntity>

    @Query("SELECT * FROM pending_notifications ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<PendingNotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingNotificationEntity)

    @Update
    suspend fun update(entity: PendingNotificationEntity)

    @Query("DELETE FROM pending_notifications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE pending_notifications SET status = :status, attempts = :attempts, lastError = :error, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, attempts: Int, error: String?, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM pending_notifications WHERE status = 'PENDING' OR status = 'FAILED'")
    fun observePendingCount(): Flow<Int>

    @Query("DELETE FROM pending_notifications WHERE status = 'SENT' AND updatedAt < :before")
    suspend fun pruneSent(before: Long)

    @Query("DELETE FROM pending_notifications WHERE id IN (SELECT id FROM pending_notifications ORDER BY createdAt DESC LIMIT -1 OFFSET 500)")
    suspend fun enforceRetention()
}

@Dao
interface DeliveryHistoryDao {
    @Query("SELECT * FROM delivery_history ORDER BY deliveredAt DESC")
    fun observeAll(): Flow<List<DeliveryHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DeliveryHistoryEntity)

    @Query("SELECT COUNT(*) FROM delivery_history WHERE deliveredAt > :since")
    suspend fun countSince(since: Long): Int

    @Query("DELETE FROM delivery_history WHERE deliveredAt < :before")
    suspend fun prune(before: Long)

    @Query("DELETE FROM delivery_history")
    suspend fun clearAll()

    @Query("SELECT * FROM delivery_history ORDER BY deliveredAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<DeliveryHistoryEntity>
}

@Dao
interface AppFilterDao {
    @Query("SELECT * FROM app_filters")
    fun observeAll(): Flow<List<AppFilterEntity>>

    @Query("SELECT * FROM app_filters")
    suspend fun getAll(): List<AppFilterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AppFilterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AppFilterEntity>)

    @Query("DELETE FROM app_filters WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("UPDATE app_filters SET isEnabled = :enabled WHERE packageName = :pkg")
    suspend fun setEnabled(pkg: String, enabled: Boolean)

    @Query("UPDATE app_filters SET isExcluded = :excluded WHERE packageName = :pkg")
    suspend fun setExcluded(pkg: String, excluded: Boolean)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): SettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entity: SettingEntity)

    @Query("SELECT * FROM settings")
    fun observeAll(): Flow<List<SettingEntity>>
}
