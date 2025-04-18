// AppDatabase.kt
/*
 * Database Room per memorizzare i segmenti dei sentieri.
 */
package com.example.treknavigator

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [TrailEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trailDao(): TrailDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trek_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
