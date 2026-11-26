package com.example.hoppie.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.hoppie.data.local.dao.ReceivedFileDao
import com.example.hoppie.data.local.entities.ReceivedFileEntity

@Database(
    entities = [ReceivedFileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HoppieDatabase : RoomDatabase() {
    abstract fun receivedFileDao(): ReceivedFileDao

    companion object {
        private const val DATABASE_NAME = "hoppie_library.db"

        @Volatile
        private var instance: HoppieDatabase? = null

        fun getInstance(context: Context): HoppieDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HoppieDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
