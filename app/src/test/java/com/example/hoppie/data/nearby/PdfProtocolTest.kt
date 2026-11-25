package com.example.hoppie.data.nearby

import com.example.hoppie.core.components.OfferedFile
import java.util.UUID
import com.example.hoppie.core.components.sha256
import com.example.hoppie.features.receive.StoredFile
import com.example.hoppie.features.receive.reviewDuplicates
import com.example.hoppie.features.send.transferProgress
import com.example.hoppie.features.transfers.CompletedTransfer
import com.example.hoppie.features.transfers.CompletedTransferFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfProtocolTest {
    private val file = OfferedFile("0", "Résumé.pdf", 1024, "application/pdf", "c".repeat(64))
    private val offer = TransferFrame(UUID.randomUUID(), PdfCommand.OFFER, true, listOf(ManifestEntry(file, Long.MIN_VALUE)))

    @Test fun offerPreservesFullPayloadIdNameSizeAndDigest() {
        assertEquals(offer, TransferFrame.decode(offer.encode()))
    }

    @Test fun controlFramesPreserveSessionIdentity() {
        for (command in PdfCommand.entries.filter { it != PdfCommand.OFFER }) {
            val frame = TransferFrame(offer.transfer, command, true)
            assertEquals(frame, TransferFrame.decode(frame.encode()))
        }
    }

    @Test fun batchRoundTripPreservesOrderAndBindsReceiptsToApprovedPayloads() {
        val text = file.copy(id = "7", name = "Notes.txt", mimeType = "text/plain")
        val third = file.copy(id = "3")
        val batch = offer.copy(legacy = false, entries = offer.entries +
            listOf(ManifestEntry(text, 99), ManifestEntry(third, 17)))
        val tx = BatchAttempt(TransferFrame.decode(batch.encode()))
        assertEquals(batch.entries, tx.offer.entries)
        val accept = tx.reply(PdfCommand.ACCEPT, listOf("3", "0"))
        tx.approve(TransferFrame.decode(accept.encode()).ids)
        assertEquals(listOf("0", "3"), tx.approved.map { it.entry.file.id })
        assertEquals(null, tx.byPayload(99))
        val saved = tx.reply(PdfCommand.SAVED, listOf("3"))
        assertEquals(tx.byPayload(17), tx.resolve(TransferFrame.decode(saved.encode())))
        assertThrows(NoSuchElementException::class.java) { tx.resolve(saved.copy(ids = listOf("7"))) }
    }

    @Test fun duplicateFileOrPayloadIdsAndAmbiguousReceiptsAreRejected() {
        val first = offer.entries.first()
        for (other in listOf(first.copy(payloadId = 5), first.copy(file = file.copy(id = "other")))) {
            assertThrows(IllegalArgumentException::class.java) {
                offer.copy(legacy = false, entries = listOf(first, other)).encode()
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            TransferFrame(offer.transfer, PdfCommand.SAVED, false, ids = listOf("0", "1")).encode()
        }
    }

    @Test fun singlePdfStillUsesOriginalWireLayout() {
        val bytes = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use {
            it.writeUTF("HOPPIE/PDF/1")
            it.writeLong(offer.transfer.mostSignificantBits)
            it.writeLong(offer.transfer.leastSignificantBits)
            it.writeUTF("OFFER")
            it.writeLong(Long.MIN_VALUE)
            it.writeUTF(file.id)
            it.writeUTF(file.name)
            it.writeLong(file.sizeBytes)
            it.writeUTF(file.sha256!!)
        }
        org.junit.Assert.assertArrayEquals(bytes.toByteArray(), offer.encode())
    }

    @Test fun byteProgressReservesCompletionForVerifiedResults() {
        assertEquals(0.5f, transferProgress(150, 300, false), 0.001f)
        assertEquals(0.625f, transferProgress(100 + 150, 100 + 300, false), 0.001f)
        assertEquals(0.999f, transferProgress(400, 400, false), 0.0001f)
        assertEquals(1f, transferProgress(400, 400, true), 0f)
    }

    @Test fun partialHistoryCountsOnlyVerifiedBytes() {
        val entries = listOf(CompletedTransferFile("report.pdf", 100, true), CompletedTransferFile("notes.txt", 300, false))
        val record = CompletedTransfer("batch", 1, entries, received = true, completed = false)
        assertEquals(100L, record.totalBytes)
        assertEquals(false, record.completed)
    }

    @Test fun sameNamedEntriesGetSeparateDestinationsAndIndependentHashes() = kotlinx.coroutines.runBlocking {
        val hash = sha256 { "first".byteInputStream() }
        val other = sha256 { "other".byteInputStream() }
        val files = listOf(file.copy(sizeBytes = 5, sha256 = hash), file.copy(id = "1", sizeBytes = 5, sha256 = other))
        val review = reviewDuplicates(files, listOf(StoredFile(file.name, 5, hash)))
        assertEquals(listOf("0"), review.remaining.map { it.id })
        val names = review.decide(skip = false).destinations(files)
        assertEquals(listOf("Résumé (1).pdf", "Résumé (2).pdf"), names.values.toList())
    }

    @Test fun malformedOrOversizedFramesAreRejected() {
        val bytes = offer.encode()
        for (invalid in listOf(bytes.copyOf(12), bytes + 0.toByte(), ByteArray(2049))) {
            assertThrows(Exception::class.java) { TransferFrame.decode(invalid) }
        }
    }

    @Test fun unsafeOfferIsRejected() {
        for (bad in listOf(file.copy(name = "../bad.pdf"), file.copy(sizeBytes = Long.MAX_VALUE),
            file.copy(sha256 = null), file.copy(name = "bad.txt"))) {
            assertThrows(IllegalArgumentException::class.java) { offer.copy(entries = listOf(ManifestEntry(bad, 42L))).encode() }
        }
    }
}
