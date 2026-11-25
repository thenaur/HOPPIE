package com.example.hoppie.data.nearby

import com.example.hoppie.core.components.FileOfferDecision
import com.google.android.gms.nearby.connection.Payload
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

/** Owns the manifest order and the in-flight operations for one authenticated offer. */
internal class BatchAttempt(val offer: TransferFrame) {
    val items = offer.entries.map(::FileExchange)
    val decision = CompletableDeferred<FileOfferDecision>()
    var approved = emptyList<FileExchange>()
        private set

    fun approve(ids: Collection<String>) {
        require(ids.isNotEmpty())
        approved = items.filter { it.entry.file.id in ids }
        require(approved.size == ids.size)
    }

    fun reply(command: PdfCommand, ids: List<String> = emptyList()) =
        TransferFrame(offer.transfer, command, offer.legacy, ids = if (offer.legacy) emptyList() else ids)

    fun resolve(frame: TransferFrame): FileExchange =
        if (offer.legacy) approved.single() else approved.single { it.entry.file.id == frame.ids.single() }

    fun byPayload(id: Long) = approved.find { it.entry.payloadId == id }
}

internal class FileExchange(val entry: ManifestEntry) {
    val started = CompletableDeferred<Unit>()
    val saved = CompletableDeferred<Unit>()
    val file = CompletableDeferred<Payload>()
    val progress = Channel<Long>(Channel.CONFLATED)
    var handle: Payload? = null

    fun byteUpdates() = flow {
        while (true) {
            val next = withTimeoutOrNull(90_000) { progress.receiveCatching() }
                ?: throw IOException("No file progress from peer")
            next.exceptionOrNull()?.let { throw it }
            if (next.isClosed) break
            emit(next.getOrThrow())
        }
    }

    fun end(error: IOException) {
        started.completeExceptionally(error)
        saved.completeExceptionally(error)
        file.completeExceptionally(error)
        progress.close(error)
    }
}
