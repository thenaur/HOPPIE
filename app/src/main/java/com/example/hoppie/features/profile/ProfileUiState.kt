package com.example.hoppie.features.profile

internal data class ProfileUiState(
    val deviceName: String = "My Phone",
    val deviceSubtitle: String = "Hoppie Device",
    val saveLocation: String = "Downloads/Hoppie",
    val storageSummary: String = "240 MB used",
    val appVersion: String = "Version 1.0"
)
