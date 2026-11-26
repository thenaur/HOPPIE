package com.example.hoppie.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfilePreferences(
    val deviceName: String = DEFAULT_DEVICE_NAME,
    val saveLocation: String = DEFAULT_SAVE_LOCATION,
    val autoSkipDuplicates: Boolean = true,
    val autoCachePreviews: Boolean = true
) {
    companion object {
        const val DEFAULT_DEVICE_NAME = "My Phone"
        const val DEFAULT_SAVE_LOCATION = "Downloads/Hoppie"
    }
}

class ProfilePreferencesStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(readCurrent())
    val preferences: StateFlow<ProfilePreferences> = _preferences.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _preferences.value = readCurrent()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    private fun readCurrent(): ProfilePreferences = ProfilePreferences(
        deviceName = prefs.getString(KEY_DEVICE_NAME, ProfilePreferences.DEFAULT_DEVICE_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: ProfilePreferences.DEFAULT_DEVICE_NAME,
        saveLocation = prefs.getString(KEY_SAVE_LOCATION, ProfilePreferences.DEFAULT_SAVE_LOCATION)
            ?.takeIf { it.isNotBlank() }
            ?: ProfilePreferences.DEFAULT_SAVE_LOCATION,
        autoSkipDuplicates = prefs.getBoolean(KEY_AUTO_SKIP_DUPLICATES, true),
        autoCachePreviews = prefs.getBoolean(KEY_AUTO_CACHE_PREVIEWS, true)
    )

    fun updateDeviceName(name: String): Boolean {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return false
        prefs.edit { putString(KEY_DEVICE_NAME, cleaned) }
        _preferences.value = readCurrent()
        return true
    }

    fun updateSaveLocation(path: String): Boolean {
        val cleaned = path.trim().trimEnd('/')
        if (cleaned.isEmpty()) return false
        prefs.edit { putString(KEY_SAVE_LOCATION, cleaned) }
        _preferences.value = readCurrent()
        return true
    }

    fun resetSaveLocation() {
        prefs.edit { putString(KEY_SAVE_LOCATION, ProfilePreferences.DEFAULT_SAVE_LOCATION) }
        _preferences.value = readCurrent()
    }

    fun setAutoSkipDuplicates(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_SKIP_DUPLICATES, enabled) }
        _preferences.value = readCurrent()
    }

    fun setAutoCachePreviews(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_CACHE_PREVIEWS, enabled) }
        _preferences.value = readCurrent()
    }

    companion object {
        private const val PREFS_NAME = "hoppie_profile_prefs"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_SAVE_LOCATION = "save_location"
        private const val KEY_AUTO_SKIP_DUPLICATES = "auto_skip_duplicates"
        private const val KEY_AUTO_CACHE_PREVIEWS = "auto_cache_previews"
    }
}
