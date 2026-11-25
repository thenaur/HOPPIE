package com.example.hoppie.features.send

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.send.components.OverallTransferProgress
import com.example.hoppie.features.send.components.TransferFileItem

@Composable
internal fun TransferFailedScreen(
    state: FileTransferState,
    onExit: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val requestExit = { confirmExit = true }
    BackHandler(onBack = requestExit)
    TransferScreenLayout(
        mascot = R.drawable.failed_mail,
        onBack = requestExit,
        backEnabled = true,
        footer = {
            FailedTransferActions(onRetry = onRetry, onCancel = requestExit)
        }
    ) {
        item(key = "overall") {
            OverallTransferProgress(state, Modifier.fillMaxWidth(0.88f))
        }
        item(key = "file-heading") { TransferFilesHeading(state.files.size) }
        items(state.files, key = { "failed:${it.file.uri}" }) {
            TransferFileItem(it, showVerificationStatus = true)
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            containerColor = Color.White,
            titleContentColor = HoppiePurple,
            textContentColor = HoppieFileSize,
            title = { Text("Leave this transfer?") },
            text = { Text("Files that were already received and verified will be kept.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    onExit()
                }) { Text("Leave transfer", color = HoppiePurple) }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("Stay", color = HoppiePurple) }
            }
        )
    }
}

@Composable
private fun FailedTransferActions(onRetry: (() -> Unit)?, onCancel: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onRetry == null) {
            Text(
                "Retry is unavailable in this version.",
                color = HoppieFileSize,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        TransferActionButton(
            label = "Retry Transfer",
            onClick = { onRetry?.invoke() },
            enabled = onRetry != null,
            modifier = Modifier.fillMaxWidth(),
            icon = R.drawable.ic_refresh,
            primary = true
        )
        TransferActionButton(
            label = "Cancel Transfer",
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            icon = R.drawable.send_remove
        )
    }
}
