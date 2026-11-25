package com.example.hoppie.features.send

import com.example.hoppie.core.components.FileOfferDecision
import com.example.hoppie.core.components.OfferedFile
import kotlinx.coroutines.flow.Flow

internal sealed interface ConnectionResponse {
    data object Completed : ConnectionResponse
    data class Failed(val requestStillPending: Boolean) : ConnectionResponse
}

/** Supplies transport operations for the endpoint that initiated the request. */
internal interface IncomingConnectionActions : OutgoingFilesActions {
    /** Return Completed only after the connection lifecycle callback confirms success. */
    suspend fun accept(endpointId: String): ConnectionResponse

    /** Return Completed after the transport finishes rejecting the endpoint. */
    suspend fun decline(endpointId: String): ConnectionResponse

}

internal interface OutgoingFilesActions {
    /**
     * Send only the manifest. Return Accepted after the receiver's explicit decision is
     * acknowledged by the protocol. Close or withdraw the offer if this call is cancelled.
     */
    suspend fun offerFiles(endpointId: String, manifest: List<OfferedFile>): FileOfferDecision

    /**
     * Start payloads on collection and release session resources when collection ends.
     * Emit Verified only after the corresponding payload and its integrity checks succeed.
     * Finish the flow after the transport has confirmed all required work. Keys are the original manifest IDs.
     */
    fun transfer(endpointId: String, files: Map<String, SelectedFile>): Flow<FileTransferEvent>

    /** Called after all acknowledgments have been consumed and the sender has recorded completion. */
    fun finishSession() {}

    /** Return true only once pending payloads have been stopped. */
    suspend fun cancelTransfer(endpointId: String): Boolean
}
