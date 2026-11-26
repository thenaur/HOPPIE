package com.example.hoppie.features.profile.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppieBlue
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppieLinkBlue
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.data.local.ProfilePreferences
import com.example.hoppie.data.nearby.nearbyPermissions
import com.example.hoppie.data.repository.ProfileStorageFormatter
import com.example.hoppie.data.repository.StorageUsageBreakdown

@Composable
internal fun EditDeviceNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var draftName by rememberSaveable(currentName) { mutableStateOf(currentName) }
    val trimmed = draftName.trim()
    val error = when {
        trimmed.isEmpty() -> "Device name cannot be empty"
        trimmed.length > 32 -> "Keep device name under 32 characters"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Edit Device Name", color = HoppiePurple, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    singleLine = true,
                    isError = error != null,
                    placeholder = {
                        Text("Enter device name", color = HoppieFileSize, style = MaterialTheme.typography.bodyMedium)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = HoppiePurple),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HoppiePurple,
                        unfocusedBorderColor = HoppieBlue,
                        cursorColor = HoppiePurple
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error, color = Color(0xFFC93B3B), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (error == null) onSave(trimmed) },
                enabled = error == null
            ) {
                Text(
                    text = "Save",
                    color = if (error == null) HoppieLinkBlue else HoppieFileSize,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = HoppieFileSize, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
internal fun SaveLocationDialog(
    currentPath: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onResetDefault: () -> Unit
) {
    var draftPath by rememberSaveable(currentPath) { mutableStateOf(currentPath) }
    val trimmed = draftPath.trim().trimEnd('/')

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Save Location", color = HoppiePurple, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Choose the folder path where HOPPIE saves received files.",
                    color = HoppieFileSize,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = draftPath,
                    onValueChange = { draftPath = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = HoppiePurple),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HoppiePurple,
                        unfocusedBorderColor = HoppieBlue,
                        cursorColor = HoppiePurple
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = {
                        draftPath = ProfilePreferences.DEFAULT_SAVE_LOCATION
                        onResetDefault()
                    }
                ) {
                    Text(
                        text = "Reset to ${ProfilePreferences.DEFAULT_SAVE_LOCATION}",
                        color = HoppieLinkBlue,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (trimmed.isNotEmpty()) onSave(trimmed) },
                enabled = trimmed.isNotEmpty()
            ) {
                Text(
                    text = "Save",
                    color = if (trimmed.isNotEmpty()) HoppieLinkBlue else HoppieFileSize,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = HoppieFileSize, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
internal fun StorageBreakdownDialog(
    usage: StorageUsageBreakdown,
    saveLocation: String,
    onClearPreviewCache: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Storage Usage", color = HoppiePurple, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StorageStatRow(
                    label = "Received Files (${usage.receivedFileCount})",
                    value = ProfileStorageFormatter.formatBytes(usage.receivedFilesBytes)
                )
                StorageStatRow(
                    label = "Cached Previews",
                    value = ProfileStorageFormatter.formatBytes(usage.previewCacheBytes)
                )
                HorizontalDivider(color = Color(0xFFECF1F8))
                StorageStatRow(
                    label = "Total Used",
                    value = ProfileStorageFormatter.formatBytes(usage.totalUsedBytes),
                    emphasize = true
                )
                Text(
                    text = "Folder: $saveLocation",
                    color = HoppieFileSize,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = onClearPreviewCache,
                    enabled = usage.previewCacheBytes > 0L,
                    colors = ButtonDefaults.buttonColors(containerColor = HoppiePurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Cached Previews", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = HoppieLinkBlue, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
private fun StorageStatRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (emphasize) HoppiePurple else HoppieFileSize,
            style = if (emphasize) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = HoppiePurple,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
internal fun AppSettingsDialog(
    autoSkipDuplicates: Boolean,
    autoCachePreviews: Boolean,
    onToggleAutoSkipDuplicates: (Boolean) -> Unit,
    onToggleAutoCachePreviews: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("App Settings", color = HoppiePurple, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingToggleRow(
                    title = "Auto-skip duplicates (SHA-256)",
                    description = "Detect identical files by SHA-256 hash before receiving.",
                    checked = autoSkipDuplicates,
                    onCheckedChange = onToggleAutoSkipDuplicates
                )
                HorizontalDivider(color = Color(0xFFECF1F8))
                SettingToggleRow(
                    title = "Auto-cache previews",
                    description = "Save rendered PDF previews locally for faster offline viewing.",
                    checked = autoCachePreviews,
                    onCheckedChange = onToggleAutoCachePreviews
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = HoppieLinkBlue, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = HoppiePurple, style = MaterialTheme.typography.labelLarge)
            Text(description, color = HoppieFileSize, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = HoppiePurple,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD0D8E8)
            )
        )
    }
}

@Composable
internal fun PermissionsStatusDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    val requiredPermissions = remember { nearbyPermissions(Build.VERSION.SDK_INT) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshKey++
    }

    val permissionStates = remember(refreshKey) {
        requiredPermissions.map { permission ->
            val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            permission to granted
        }
    }
    val missingPermissions = permissionStates.filterNot { it.second }.map { it.first }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Nearby Permissions", color = HoppiePurple, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "HOPPIE uses Google Nearby Connections (Bluetooth & Wi-Fi) for offline peer-to-peer transfers.",
                    color = HoppieFileSize,
                    style = MaterialTheme.typography.bodyMedium
                )
                permissionStates.forEach { (permission, granted) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3F6FC))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = friendlyPermissionLabel(permission),
                            color = HoppiePurple,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (granted) Color(0xFFE3F5EA) else Color(0xFFFDECEC))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (granted) "Granted" else "Required",
                                color = if (granted) Color(0xFF1E7E46) else Color(0xFFC93B3B),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                if (missingPermissions.isNotEmpty()) {
                    Button(
                        onClick = { launcher.launch(missingPermissions.toTypedArray()) },
                        colors = ButtonDefaults.buttonColors(containerColor = HoppiePurple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Request Missing Permissions", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Text(
                        text = "All required Nearby Connections permissions are granted.",
                        color = Color(0xFF1E7E46),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                TextButton(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        runCatching { context.startActivity(intent) }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Open System App Settings", color = HoppieLinkBlue, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = HoppieLinkBlue, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

private fun friendlyPermissionLabel(permission: String): String = when (permission) {
    Manifest.permission.BLUETOOTH_SCAN -> "Bluetooth Scan"
    Manifest.permission.BLUETOOTH_ADVERTISE -> "Bluetooth Advertise"
    Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth Connect"
    Manifest.permission.NEARBY_WIFI_DEVICES -> "Nearby Wi-Fi Devices"
    Manifest.permission.ACCESS_FINE_LOCATION -> "Precise Location (Discovery)"
    Manifest.permission.ACCESS_COARSE_LOCATION -> "Approximate Location (Discovery)"
    "android.permission.ACCESS_LOCAL_NETWORK" -> "Local Network Access"
    else -> permission.substringAfterLast('.')
}

@Composable
internal fun AboutHoppieDialog(
    appVersion: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.hoppie_head),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text("HOPPIE", color = HoppiePurple, style = MaterialTheme.typography.titleLarge)
                Text(appVersion, color = HoppieFileSize, style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            Text(
                text = "Hybrid Offline Peer-to-Peer Information Exchange lets students and instructors discover nearby devices via Bluetooth and share academic files over local Wi-Fi with SHA-256 integrity verification, duplicate detection, in-app previews, and store-and-forward redistribution — no internet required.",
                color = HoppieFileSize,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = HoppieLinkBlue, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
