package com.example.hoppie.di

import android.content.Context
import com.example.hoppie.data.local.database.HoppieDatabase
import com.example.hoppie.data.repository.LibraryRepository

object LibraryContainer {
    @Volatile
    private var libraryRepo: LibraryRepository? = null

    fun libraryRepository(context: Context): LibraryRepository {
        return libraryRepo ?: synchronized(this) {
            libraryRepo ?: LibraryRepository(
                context = context.applicationContext,
                dao = HoppieDatabase.getInstance(context.applicationContext).receivedFileDao()
            ).also { libraryRepo = it }
        }
    }
}
