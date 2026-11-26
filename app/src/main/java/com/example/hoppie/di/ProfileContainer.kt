package com.example.hoppie.di

import android.content.Context
import com.example.hoppie.data.local.ProfilePreferencesStore
import com.example.hoppie.data.repository.ProfileRepository

object ProfileContainer {
    @Volatile
    private var preferencesStore: ProfilePreferencesStore? = null

    @Volatile
    private var profileRepo: ProfileRepository? = null

    fun preferencesStore(context: Context): ProfilePreferencesStore {
        return preferencesStore ?: synchronized(this) {
            preferencesStore ?: ProfilePreferencesStore(context.applicationContext).also {
                preferencesStore = it
            }
        }
    }

    fun profileRepository(context: Context): ProfileRepository {
        return profileRepo ?: synchronized(this) {
            profileRepo ?: ProfileRepository(
                context = context.applicationContext,
                preferencesStore = preferencesStore(context)
            ).also { profileRepo = it }
        }
    }
}
