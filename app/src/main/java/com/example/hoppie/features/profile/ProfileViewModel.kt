package com.example.hoppie.features.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoppie.data.repository.StorageUsageBreakdown
import com.example.hoppie.di.ProfileContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfileContainer.profileRepository(application)

    private val storageUsage = MutableStateFlow(StorageUsageBreakdown())

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.preferences,
        storageUsage
    ) { prefs, usage ->
        ProfileUiState(
            deviceName = prefs.deviceName,
            saveLocation = prefs.saveLocation,
            storageUsage = usage,
            autoSkipDuplicates = prefs.autoSkipDuplicates,
            autoCachePreviews = prefs.autoCachePreviews
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ProfileUiState()
    )

    init {
        refreshStorage()
    }

    fun refreshStorage() {
        viewModelScope.launch {
            storageUsage.value = repository.calculateStorageUsage()
        }
    }

    fun updateDeviceName(name: String): Boolean = repository.updateDeviceName(name)

    fun updateSaveLocation(path: String): Boolean = repository.updateSaveLocation(path)

    fun resetSaveLocation() = repository.resetSaveLocation()

    fun setAutoSkipDuplicates(enabled: Boolean) = repository.setAutoSkipDuplicates(enabled)

    fun setAutoCachePreviews(enabled: Boolean) = repository.setAutoCachePreviews(enabled)

    fun clearPreviewCache(onCleared: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val cleared = repository.clearPreviewCache()
            storageUsage.value = repository.calculateStorageUsage()
            onCleared(cleared)
        }
    }
}
