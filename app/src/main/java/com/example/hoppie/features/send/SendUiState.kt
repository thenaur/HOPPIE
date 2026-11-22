package com.example.hoppie.features.send

import android.net.Uri
import com.example.hoppie.core.components.FileCategory
import java.text.NumberFormat

internal const val MAX_FILE_BYTES = 50L * 1024 * 1024

internal data class SelectedFile(
    val uri: Uri,
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val category: FileCategory
)

internal data class SendUiState(
    val files: List<SelectedFile> = emptyList(),
    val validationMessages: List<String> = emptyList(),
    val isCheckingFiles: Boolean = false
) {
    val totalBytes: Long get() = files.sumOf { it.sizeBytes }
}

internal fun formatFileSize(bytes: Long): String {
    val formatter = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
    return when {
        bytes >= 1024L * 1024 -> "${formatter.format(bytes / (1024.0 * 1024))} MB"
        bytes >= 1024L -> "${formatter.format(bytes / 1024.0)} KB"
        else -> "${formatter.format(bytes)} B"
    }
}
