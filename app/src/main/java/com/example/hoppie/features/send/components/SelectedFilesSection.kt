package com.example.hoppie.features.send.components

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.features.send.SendUiState

internal fun LazyListScope.selectedFilesSection(state: SendUiState, onRemove: (Uri) -> Unit) {
    item(key = "selected-heading") {
        Text(
            text = "Selected files (${state.files.size})",
            modifier = Modifier.padding(horizontal = 30.dp).padding(bottom = 6.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = HoppiePurple
        )
    }
    if (state.files.isEmpty() || state.isCheckingFiles) {
        item(key = "selection-status") {
            Text(
                text = if (state.isCheckingFiles) "Checking files…" else "Upload files to add them here.",
                modifier = Modifier.padding(horizontal = 30.dp).padding(vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyLarge,
                color = HoppieFileSize
            )
        }
    }
    if (state.validationMessages.isNotEmpty()) {
        item(key = "validation") {
            Text(
                text = state.validationMessages.joinToString("\n"),
                modifier = Modifier.padding(horizontal = 30.dp).padding(vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = HoppieFileSize
            )
        }
    }
    items(state.files, key = { "file:${it.uri}" }) { file ->
        SelectedFileItem(
            file = file,
            onRemove = { onRemove(file.uri) },
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 10.dp)
        )
    }
}
