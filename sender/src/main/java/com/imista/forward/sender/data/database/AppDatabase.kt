package com.imista.forward.sender.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.imista.forward.sender.data.dao.*
import com.imista.forward.sender.data.entity.*

@Database(
    entities = [PendingNotificationEntity::class, DeliveryHistoryEntity::class, AppFilterEntity::class, SettingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingDao(): PendingNotificationDao
    abstract fun historyDao(): DeliveryHistoryDao
    abstract fun filterDao(): AppFilterDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "imista_sender.db")
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
