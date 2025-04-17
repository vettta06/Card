package com.example.card.Stat

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Stats::class], version = 1)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile
        private var INSTANCE: StatsDatabase? = null

        fun getDatabase(context: Context): StatsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StatsDatabase::class.java,
                    "stats.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}