package com.example.network_logger_lib.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NetworkLogEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NetworkLogDatabase : RoomDatabase() {

    abstract fun networkLogDao(): NetworkLogDao

    companion object {
        @Volatile
        private var instance: NetworkLogDatabase? = null

        fun getInstance(context: Context): NetworkLogDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NetworkLogDatabase::class.java,
                    "network_logger.db",
                ).build().also { instance = it }
            }
        }
    }
}
