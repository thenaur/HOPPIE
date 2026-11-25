package com.example.hoppie.data.nearby

import org.junit.Assert.assertEquals
import org.junit.Test

class FinalReceiptTest {
    @Test fun disconnectDuringFinalReceiptWaitsForActualDeliveryResult() {
        for (success in listOf(true, false)) {
            val receipt = FinalReceipt()
            val results = mutableListOf<Boolean>()
            receipt.sending()
            receipt.onDisconnect { results += it }
            assertEquals(emptyList<Boolean>(), results)
            receipt.finish(success)
            receipt.finish(false)
            assertEquals(listOf(success), results)
        }
    }

    @Test fun completedReceiptBelongsOnlyToItsOwnBatch() {
        val receipt = FinalReceipt()
        val results = mutableListOf<Boolean>()
        receipt.sending()
        receipt.finish(true)
        receipt.onDisconnect { results += it }
        receipt.reset()
        receipt.onDisconnect { results += it }
        assertEquals(listOf(true, false), results)
    }
}
