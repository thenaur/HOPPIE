package com.example.hoppie.features.profile.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.theme.HoppieBlue
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppieLinkBlue
import com.example.hoppie.core.theme.HoppiePurple

@Composable
internal fun EditDeviceNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var draftName by rememberSaveable(currentName) { mutableStateOf(currentName) }
    val trimmedName = draftName.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Edit Device Name",
                color = HoppiePurple,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            OutlinedTextField(
                value = draftName,
                onValueChange = { draftName = it },
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Enter device name",
                        color = HoppieFileSize,
                        style = MaterialTheme.typography.bodyMedium
                    )
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
        },
        confirmButton = {
            TextButton(
                onClick = { if (trimmedName.isNotEmpty()) onSave(trimmedName) },
                enabled = trimmedName.isNotEmpty()
            ) {
                Text(
                    text = "Save",
                    color = if (trimmedName.isNotEmpty()) HoppieLinkBlue else HoppieFileSize,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = HoppieFileSize,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
