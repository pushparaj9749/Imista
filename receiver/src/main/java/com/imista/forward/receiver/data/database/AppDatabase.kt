package com.imista.forward.receiver.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.imista.forward.receiver.data.dao.ReceivedDao
import com.imista.forward.receiver.data.dao.SettingDao
import com.imista.forward.receiver.data.entity.ReceivedNotificationEntity
import com.imista.forward.receiver.data.entity.SettingEntity

@Database(entities = [ReceivedNotificationEntity::class, SettingEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun receivedDao(): ReceivedDao
    abstract fun settingDao(): SettingDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "imista_receiver.db")
                .fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
