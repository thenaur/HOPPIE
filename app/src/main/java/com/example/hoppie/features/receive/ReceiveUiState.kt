package com.example.hoppie.features.receive

import android.net.Uri
import com.example.hoppie.core.components.FileCategory
import com.example.hoppie.core.components.HoppieNotice
import com.example.hoppie.core.components.OfferedFile
import com.example.hoppie.features.send.FileTransferState
import com.example.hoppie.features.send.SelectedFile

internal sealed interface DiscoveryStatus {
    data object Starting : DiscoveryStatus
    data object Scanning : DiscoveryStatus
    data object Completed : DiscoveryStatus
    data object Cancelled : DiscoveryStatus
    data class Connecting(val peerName: String) : DiscoveryStatus
    data class Verifying(val peerName: String) : DiscoveryStatus
    data class AwaitingConfirmation(val peerName: String) : DiscoveryStatus
    data object Declining : DiscoveryStatus
    data class Connected(val peerName: String) : DiscoveryStatus
    data object SessionActive : DiscoveryStatus
    data class Unavailable(val message: String) : DiscoveryStatus
}

internal sealed interface DeviceConnection {
    data object Available : DeviceConnection
    data object Connecting : DeviceConnection
    data object Connected : DeviceConnection
    data class Failed(val message: String) : DeviceConnection
}

internal data class NearbyDevice(
    val id: String,
    val name: String,
    val connection: DeviceConnection = DeviceConnection.Available
)

internal data class ReceiveUiState(
    val discovery: DiscoveryStatus,
    val devices: List<NearbyDevice> = emptyList(),
    val incoming: IncomingFilesState? = null,
    val transfer: FileTransferState? = null,
    val notice: String? = null,
    val cardNotice: HoppieNotice? = null
)

internal data class IncomingFileOffer(
    val sessionId: String,
    val senderName: String,
    val files: List<OfferedFile>
) {
    val totalBytes: Long get() = files.sumOf { it.sizeBytes }
}

internal data class IncomingFilesState(
    val offer: IncomingFileOffer,
    val busy: Boolean = false,
    val accepting: Boolean = false,
    val error: String? = null,
    val duplicateReview: DuplicateFileReview? = null,
    val reviewingDuplicates: Boolean = false
)

/** The URI is an in-memory progress key for this offer. The transport owns the destination file. */
internal fun OfferedFile.asTransferFile(): SelectedFile = SelectedFile(
    uri = Uri.Builder().scheme("hoppie-incoming").appendPath(id).build(),
    name = name,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    category = FileCategory.from(name, mimeType)
)
