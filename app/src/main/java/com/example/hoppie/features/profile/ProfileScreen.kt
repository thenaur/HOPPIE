package com.example.hoppie.features.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HOPPIETheme
import com.example.hoppie.features.profile.components.EditDeviceNameDialog
import com.example.hoppie.features.profile.components.ProfileHeader
import com.example.hoppie.features.profile.components.ProfileIdentitySection
import com.example.hoppie.features.profile.components.ProfileSettingItem
import com.example.hoppie.features.profile.components.ProfileSettingsCard

private val ProfileScreenBackground = Color(0xFFF7F9FC)

@Composable
internal fun ProfileScreen() {
    val context = LocalContext.current
    val defaults = ProfileUiState()
    var deviceName by rememberSaveable { mutableStateOf(defaults.deviceName) }
    var showEditNameDialog by rememberSaveable { mutableStateOf(false) }

    fun showComingSoon(label: String) {
        Toast.makeText(context, "$label coming soon", Toast.LENGTH_SHORT).show()
    }

    val deviceSettings = listOf(
        ProfileSettingItem(
            title = "Device Name",
            subtitle = deviceName,
            iconRes = R.drawable.phone_icon,
            iconSize = 26.dp,
            onClick = { showEditNameDialog = true }
        ),
        ProfileSettingItem(
            title = "Save Location",
            subtitle = defaults.saveLocation,
            iconRes = R.drawable.ic_profile_folder,
            iconSize = 24.dp,
            onClick = { showComingSoon("Save Location") }
        ),
        ProfileSettingItem(
            title = "Storage",
            subtitle = defaults.storageSummary,
            iconRes = R.drawable.ic_profile_storage,
            iconSize = 24.dp,
            onClick = { showComingSoon("Storage") }
        )
    )

    val appSettings = listOf(
        ProfileSettingItem(
            title = "App Settings",
            iconRes = R.drawable.settings_icon,
            iconSize = 28.dp,
            onClick = { showComingSoon("App Settings") }
        ),
        ProfileSettingItem(
            title = "Permissions",
            iconRes = R.drawable.ic_profile_shield_check,
            iconSize = 24.dp,
            onClick = { showComingSoon("Permissions") }
        ),
        ProfileSettingItem(
            title = "About Hoppie",
            subtitle = defaults.appVersion,
            iconRes = R.drawable.info_icon,
            iconSize = 32.dp,
            onClick = { showComingSoon("About Hoppie") }
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileScreenBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileHeader()
            Spacer(Modifier.height(14.dp))
            ProfileIdentitySection(
                deviceName = deviceName,
                deviceSubtitle = defaults.deviceSubtitle,
                onEditDeviceName = { showEditNameDialog = true }
            )
            Spacer(Modifier.height(22.dp))
            ProfileSettingsCard(items = deviceSettings)
            Spacer(Modifier.height(16.dp))
            ProfileSettingsCard(items = appSettings)
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showEditNameDialog) {
        EditDeviceNameDialog(
            currentName = deviceName,
            onDismiss = { showEditNameDialog = false },
            onSave = { updatedName ->
                deviceName = updatedName
                showEditNameDialog = false
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    HOPPIETheme {
        ProfileScreen()
    }
}
