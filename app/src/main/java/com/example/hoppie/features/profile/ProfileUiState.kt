package com.example.hoppie.features.profile

import com.example.hoppie.data.local.ProfilePreferences
import com.example.hoppie.data.repository.ProfileStorageFormatter
import com.example.hoppie.data.repository.StorageUsageBreakdown

data class ProfileUiState(
    val deviceName: String = ProfilePreferences.DEFAULT_DEVICE_NAME,
    val deviceSubtitle: String = "Hoppie Device",
    val saveLocation: String = ProfilePreferences.DEFAULT_SAVE_LOCATION,
    val storageUsage: StorageUsageBreakdown = StorageUsageBreakdown(),
    val autoSkipDuplicates: Boolean = true,
    val autoCachePreviews: Boolean = true,
    val appVersion: String = "Version 1.0"
) {
    val storageSummary: String
        get() = "${ProfileStorageFormatter.formatBytes(storageUsage.totalUsedBytes)} used"
}

/**
 * Sample state for Compose @Preview rendering only (matches mockup).
 */
internal val sampleProfilePreviewState = ProfileUiState(
    deviceName = "My Phone",
    deviceSubtitle = "Hoppie Device",
    saveLocation = "Downloads/Hoppie",
    storageUsage = StorageUsageBreakdown(
        receivedFilesBytes = 240L * 1024L * 1024L,
        receivedFileCount = 12,
        previewCacheBytes = 0L
    ),
    appVersion = "Version 1.0"
)
