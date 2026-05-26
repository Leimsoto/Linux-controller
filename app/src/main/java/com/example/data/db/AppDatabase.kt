package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.PCProfile
import com.example.data.model.QuickCommand

@Database(entities = [PCProfile::class, QuickCommand::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun remoteControlDao(): RemoteControlDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "linux_remote_control_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
