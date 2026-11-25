package com.example.hoppie.features.send

import android.net.Uri

internal sealed interface FileTransferEvent {
    data class BytesSent(val uri: Uri, val bytes: Long) : FileTransferEvent
    data class Verifying(val uri: Uri) : FileTransferEvent
    data class Verified(val uri: Uri, val savedUri: Uri? = null) : FileTransferEvent
    data object Cancelled : FileTransferEvent
}

internal enum class TransferStatus { Preparing, Transferring, Verifying, Completed, Failed, Cancelled }

internal data class TransferFile(
    val file: SelectedFile,
    val bytesSent: Long = 0,
    val verifying: Boolean = false,
    val verified: Boolean = false,
    val savedUri: Uri? = null,
    val originalName: String = file.name
) {
    val progress: Float get() = transferProgress(bytesSent, file.sizeBytes, verified)
}

internal data class FileTransferState(
    val files: List<TransferFile>,
    val status: TransferStatus = TransferStatus.Preparing,
    val cancelling: Boolean = false,
    val message: String? = null,
    val recipientName: String? = null
) {
    val verifiedCount get() = files.count { it.verified }
    val verifiedBytes get() = files.filter { it.verified }.sumOf { it.file.sizeBytes }
    val active: Boolean
        get() = status == TransferStatus.Preparing || status == TransferStatus.Transferring ||
            status == TransferStatus.Verifying
    val bytesSent: Long get() = files.sumOf { it.bytesSent }
    val totalBytes: Long get() = files.sumOf { it.file.sizeBytes }
    val progress: Float get() = transferProgress(bytesSent, totalBytes, status == TransferStatus.Completed)

    fun applyEvent(event: FileTransferEvent): FileTransferState {
        if (!active) return this
        val uri = when (event) {
            is FileTransferEvent.BytesSent -> event.uri
            is FileTransferEvent.Verifying -> event.uri
            is FileTransferEvent.Verified -> event.uri
            FileTransferEvent.Cancelled -> return copy(status = TransferStatus.Cancelled, cancelling = false)
        }
        val index = files.indexOfFirst { it.file.uri == uri }
        if (index < 0 || files[index].verified) return this
        val current = files[index]
        val updated = when (event) {
            is FileTransferEvent.BytesSent ->
                current.copy(bytesSent = event.bytes.coerceIn(current.bytesSent, current.file.sizeBytes))
            is FileTransferEvent.Verifying -> current.copy(verifying = true)
            is FileTransferEvent.Verified ->
                current.copy(bytesSent = current.file.sizeBytes, verifying = false, verified = true, savedUri = event.savedUri)
            FileTransferEvent.Cancelled -> return this
        }
        val nextFiles = files.toMutableList().apply { set(index, updated) }
        val nextStatus = if (nextFiles.all { it.verifying || it.verified }) {
            TransferStatus.Verifying
        } else {
            TransferStatus.Transferring
        }
        return copy(files = nextFiles, status = nextStatus)
    }
}

internal val FileTransferState?.isPayloadTransferActive: Boolean
    get() = this != null && active && files.isNotEmpty()

internal fun transferProgress(bytes: Long, total: Long, verified: Boolean): Float = when {
    verified -> 1f
    total == 0L -> 0f
    else -> (bytes.toDouble() / total).toFloat().coerceIn(0f, 0.999f)
}
