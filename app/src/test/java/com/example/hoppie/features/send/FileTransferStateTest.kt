package com.example.hoppie.features.send

import org.junit.Assert.assertFalse
import org.junit.Test

class FileTransferStateTest {
    @Test
    fun emptyOrNullTransferStateIsNeverActivePayloadTransfer() {
        val nullState: FileTransferState? = null
        assertFalse(nullState.isPayloadTransferActive)
        for (status in TransferStatus.entries) {
            assertFalse(FileTransferState(emptyList(), status).isPayloadTransferActive)
        }
    }
}
