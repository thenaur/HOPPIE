package com.example.hoppie.features.send

import android.net.Uri
import com.example.hoppie.core.components.FileCategory
import com.example.hoppie.core.components.HoppieNotice
import java.text.NumberFormat

internal const val MAX_FILE_BYTES = 50L * 1024 * 1024

internal data class SelectedFile(
    val uri: Uri,
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val category: FileCategory
)

internal class IncomingConnectionRequest(val endpointId: String, val deviceName: String)

internal enum class IncomingConnectionStatus { AwaitingDecision, Accepting, Declining }

internal data class IncomingConnectionUiState(
    val request: IncomingConnectionRequest,
    val status: IncomingConnectionStatus = IncomingConnectionStatus.AwaitingDecision,
    val error: String? = null
)

internal data class SendUiState(
    val files: List<SelectedFile> = emptyList(),
    val validationMessages: List<String> = emptyList(),
    val isCheckingFiles: Boolean = false,
    val incomingConnection: IncomingConnectionUiState? = null,
    val connectionNotice: HoppieNotice? = null,
    val transfer: FileTransferState? = null,
    val waitingForReceiver: Boolean = false
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
