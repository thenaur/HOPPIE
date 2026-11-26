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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoppie.R
import com.example.hoppie.core.theme.HOPPIETheme
import com.example.hoppie.data.repository.ProfileStorageFormatter
import com.example.hoppie.features.profile.components.AboutHoppieDialog
import com.example.hoppie.features.profile.components.AppSettingsDialog
import com.example.hoppie.features.profile.components.EditDeviceNameDialog
import com.example.hoppie.features.profile.components.PermissionsStatusDialog
import com.example.hoppie.features.profile.components.ProfileHeader
import com.example.hoppie.features.profile.components.ProfileIdentitySection
import com.example.hoppie.features.profile.components.ProfileSettingItem
import com.example.hoppie.features.profile.components.ProfileSettingsCard
import com.example.hoppie.features.profile.components.SaveLocationDialog
import com.example.hoppie.features.profile.components.StorageBreakdownDialog

private val ProfileScreenBackground = Color(0xFFF6F8FC)

private enum class ActiveProfileDialog {
    EditDeviceName,
    SaveLocation,
    StorageBreakdown,
    AppSettings,
    Permissions,
    AboutHoppie
}

@Composable
internal fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var activeDialog by rememberSaveable { mutableStateOf<ActiveProfileDialog?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshStorage()
    }

    ProfileScreenContent(
        state = state,
        onEditDeviceName = { activeDialog = ActiveProfileDialog.EditDeviceName },
        onOpenSaveLocation = { activeDialog = ActiveProfileDialog.SaveLocation },
        onOpenStorage = {
            viewModel.refreshStorage()
            activeDialog = ActiveProfileDialog.StorageBreakdown
        },
        onOpenAppSettings = { activeDialog = ActiveProfileDialog.AppSettings },
        onOpenPermissions = { activeDialog = ActiveProfileDialog.Permissions },
        onOpenAbout = { activeDialog = ActiveProfileDialog.AboutHoppie }
    )

    when (activeDialog) {
        ActiveProfileDialog.EditDeviceName -> EditDeviceNameDialog(
            currentName = state.deviceName,
            onDismiss = { activeDialog = null },
            onSave = { updated ->
                if (viewModel.updateDeviceName(updated)) {
                    activeDialog = null
                }
            }
        )

        ActiveProfileDialog.SaveLocation -> SaveLocationDialog(
            currentPath = state.saveLocation,
            onDismiss = { activeDialog = null },
            onSave = { updated ->
                if (viewModel.updateSaveLocation(updated)) {
                    activeDialog = null
                }
            },
            onResetDefault = {
                viewModel.resetSaveLocation()
                Toast.makeText(context, "Save location reset to default", Toast.LENGTH_SHORT).show()
            }
        )

        ActiveProfileDialog.StorageBreakdown -> StorageBreakdownDialog(
            usage = state.storageUsage,
            saveLocation = state.saveLocation,
            onClearPreviewCache = {
                viewModel.clearPreviewCache { cleared ->
                    Toast.makeText(
                        context,
                        "Cleared ${ProfileStorageFormatter.formatBytes(cleared)} of cached previews",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { activeDialog = null }
        )

        ActiveProfileDialog.AppSettings -> AppSettingsDialog(
            autoSkipDuplicates = state.autoSkipDuplicates,
            autoCachePreviews = state.autoCachePreviews,
            onToggleAutoSkipDuplicates = viewModel::setAutoSkipDuplicates,
            onToggleAutoCachePreviews = viewModel::setAutoCachePreviews,
            onDismiss = { activeDialog = null }
        )

        ActiveProfileDialog.Permissions -> PermissionsStatusDialog(
            onDismiss = { activeDialog = null }
        )

        ActiveProfileDialog.AboutHoppie -> AboutHoppieDialog(
            appVersion = state.appVersion,
            onDismiss = { activeDialog = null }
        )

        null -> Unit
    }
}

@Composable
internal fun ProfileScreenContent(
    state: ProfileUiState,
    onEditDeviceName: () -> Unit,
    onOpenSaveLocation: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val deviceSettings = listOf(
        ProfileSettingItem(
            title = "Device Name",
            subtitle = state.deviceName,
            iconRes = R.drawable.phone_icon,
            iconSize = 26.dp,
            onClick = onEditDeviceName
        ),
        ProfileSettingItem(
            title = "Save Location",
            subtitle = state.saveLocation,
            iconRes = R.drawable.ic_profile_folder,
            iconSize = 24.dp,
            onClick = onOpenSaveLocation
        ),
        ProfileSettingItem(
            title = "Storage",
            subtitle = state.storageSummary,
            iconRes = R.drawable.ic_profile_storage,
            iconSize = 24.dp,
            onClick = onOpenStorage
        )
    )

    val appSettings = listOf(
        ProfileSettingItem(
            title = "App Settings",
            iconRes = R.drawable.settings_icon,
            iconSize = 28.dp,
            onClick = onOpenAppSettings
        ),
        ProfileSettingItem(
            title = "Permissions",
            iconRes = R.drawable.ic_profile_shield_check,
            iconSize = 24.dp,
            onClick = onOpenPermissions
        ),
        ProfileSettingItem(
            title = "About Hoppie",
            subtitle = state.appVersion,
            iconRes = R.drawable.info_icon,
            iconSize = 32.dp,
            onClick = onOpenAbout
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
                deviceName = state.deviceName,
                deviceSubtitle = state.deviceSubtitle,
                onEditDeviceName = onEditDeviceName
            )
            Spacer(Modifier.height(22.dp))
            ProfileSettingsCard(items = deviceSettings)
            Spacer(Modifier.height(16.dp))
            ProfileSettingsCard(items = appSettings)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    HOPPIETheme {
        ProfileScreenContent(
            state = sampleProfilePreviewState,
            onEditDeviceName = {},
            onOpenSaveLocation = {},
            onOpenStorage = {},
            onOpenAppSettings = {},
            onOpenPermissions = {},
            onOpenAbout = {}
        )
    }
}
