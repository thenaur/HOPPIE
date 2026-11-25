package com.example.hoppie.data.nearby

import android.content.Context
import com.example.hoppie.core.components.FileOfferDecision
import com.example.hoppie.core.components.OfferedFile
import com.example.hoppie.features.receive.IncomingFileOffer
import com.example.hoppie.features.receive.IncomingFilesActions
import com.example.hoppie.features.receive.asTransferFile
import com.example.hoppie.features.send.FileTransferEvent
import com.example.hoppie.features.send.OutgoingFilesActions
import com.example.hoppie.features.send.SelectedFile
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

/** One authenticated endpoint. SDK callbacks and protocol transitions are serialized on main. */
private class PeerCancelledException : IOException("Transfer cancelled")

internal class PdfLink(
    private val app: Context,
    private val client: ConnectionsClient,
    private val peer: Peer,
    private val role: NearbyRole,
    private val finishSession: () -> Unit,
    private val authenticated: () -> Boolean
) : PayloadCallback(), IncomingFilesActions {
    private val store = ReceivedPdfStore(app)
    private val pending = MutableStateFlow<IncomingFileOffer?>(null)
    val incoming = pending.asStateFlow()
    @Volatile private var active: BatchAttempt? = null
    val finalReceipt = FinalReceipt()
    private val controls = mutableMapOf<Long, CompletableDeferred<Unit>>()

    private fun send(payload: Payload, failure: () -> Unit) {
        if (!authenticated()) throw IOException("Peer disconnected")
        client.sendPayload(peer.id, payload).addOnFailureListener { failure() }.addOnCanceledListener { failure() }
    }

    private suspend fun control(frame: TransferFrame) {
        val payload = Payload.fromBytes(frame.encode())
        val delivered = CompletableDeferred<Unit>()
        controls[payload.id] = delivered
        try {
            send(payload) { delivered.completeExceptionally(IOException("Control message failed")) }
            delivered.waitFor(20_000)
        } finally {
            controls.remove(payload.id)
        }
    }

    fun sender(selected: List<SelectedFile>): OutgoingFilesActions = object : OutgoingFilesActions {
        override suspend fun offerFiles(endpointId: String, manifest: List<OfferedFile>): FileOfferDecision {
            check(role == NearbyRole.Advertiser && authenticated() && endpointId == peer.id && active == null)
            require(manifest.size == selected.size && manifest.size in 1..TransferFrame.MAX_FILES)
            val payloads = mutableListOf<Payload>()
            val tx = try {
                val entries = manifest.zip(selected).map { (file, source) ->
                    val descriptor = checkNotNull(app.contentResolver.openFileDescriptor(source.uri, "r"))
                    val payload = try { Payload.fromFile(descriptor) } catch (error: Exception) {
                        descriptor.close()
                        throw error
                    }
                    payloads.add(payload)
                    ManifestEntry(file, payload.id)
                }
                val legacy = manifest.singleOrNull()?.name?.endsWith(".pdf", true) == true
                BatchAttempt(TransferFrame(UUID.randomUUID(), PdfCommand.OFFER, legacy, entries))
            } catch (error: Exception) {
                payloads.forEach { it.close() }
                throw error
            }
            tx.items.zip(payloads).forEach { (item, payload) -> item.handle = payload }
            active = tx
            try {
                control(tx.offer)
                val answer = tx.decision.waitFor(300_000)
                if (tx.approved.isEmpty()) release(tx)
                return answer
            } catch (error: Exception) {
                abort(tx, notify = true)
                throw error
            }
        }

        override fun transfer(endpointId: String, files: Map<String, SelectedFile>) = flow {
            val tx = checkNotNull(active)
            var cancelledByPeer = false
            try {
                check(endpointId == peer.id && tx.approved.isNotEmpty())
                check(tx.approved.map { it.entry.file.id }.toSet() == files.keys)
                for (item in tx.approved) {
                    val id = item.entry.file.id
                    val uri = files.getValue(id).uri
                    item.started.complete(Unit)
                    control(tx.reply(PdfCommand.START, listOf(id)))
                    send(checkNotNull(item.handle)) { abort(tx, notify = true) }
                    item.byteUpdates().collect { emit(FileTransferEvent.BytesSent(uri, it)) }
                    emit(FileTransferEvent.Verifying(uri))
                    item.saved.waitFor(90_000)
                    emit(FileTransferEvent.Verified(uri))
                    closeHandle(item)
                }
                release(tx)
            } catch (_: PeerCancelledException) {
                cancelledByPeer = true
                emit(FileTransferEvent.Cancelled)
            } finally {
                abort(tx, notify = !cancelledByPeer)
            }
        }

        override fun finishSession() {
            if (active == null && authenticated()) this@PdfLink.finishSession()
        }

        override suspend fun cancelTransfer(endpointId: String) = cancel()
    }

    override suspend fun inspectDestination(offer: IncomingFileOffer) = store.inspect(offer.files)

    private fun incomingAttempt(offer: IncomingFileOffer): BatchAttempt = checkNotNull(active).also {
        check(role == NearbyRole.Discoverer && it.offer.transfer.toString() == offer.sessionId)
    }

    suspend fun rejectBusy(offer: IncomingFileOffer) {
        val tx = incomingAttempt(offer)
        try {
            decline(offer)
        } catch (error: Exception) {
            abort(tx, notify = true)
            if (error is CancellationException) throw error
        }
    }

    override suspend fun decline(offer: IncomingFileOffer) {
        val tx = incomingAttempt(offer)
        finalReceipt.sending()
        try {
            control(tx.reply(PdfCommand.DECLINE))
            release(tx)
            finalReceipt.finish(delivered = true)
        } catch (error: Exception) {
            finalReceipt.finish(delivered = false)
            throw error
        }
    }

    override suspend fun accept(offer: IncomingFileOffer, destinations: Map<String, String>): Flow<FileTransferEvent> {
        val tx = incomingAttempt(offer)
        if (destinations.isEmpty()) {
            finalReceipt.sending()
            try {
                control(tx.reply(PdfCommand.SKIP))
                release(tx)
                finalReceipt.finish(delivered = true)
            } catch (error: Exception) {
                finalReceipt.finish(delivered = false)
                throw error
            }
            return emptyFlow()
        }
        try {
            tx.approve(destinations.keys)
            control(tx.reply(PdfCommand.ACCEPT, destinations.keys.toList()))
            tx.approved.first().started.waitFor(30_000)
        } catch (error: Exception) {
            abort(tx, notify = true)
            throw error
        }
        return flow {
            var cancelledByPeer = false
            try {
                for (item in tx.approved) {
                    val file = item.entry.file
                    val uri = file.asTransferFile().uri
                    item.started.waitFor(30_000)
                    item.byteUpdates().collect { emit(FileTransferEvent.BytesSent(uri, it)) }
                    emit(FileTransferEvent.Verifying(uri))
                    val received = checkNotNull(item.file.waitFor(10_000).asFile())
                    val saved = store.save(file, destinations.getValue(file.id), { active === tx && authenticated() }) {
                        store.open(received)
                    }
                    emit(FileTransferEvent.Verified(uri, saved))
                    closeHandle(item)
                    item.saved.complete(Unit)
                    if (item === tx.approved.last()) finalReceipt.sending()
                    control(tx.reply(PdfCommand.SAVED, listOf(file.id)))
                }
                release(tx)
                finalReceipt.finish(delivered = true)
            } catch (_: PeerCancelledException) {
                cancelledByPeer = true
                emit(FileTransferEvent.Cancelled)
            } finally {
                abort(tx, notify = !cancelledByPeer)
                finalReceipt.finish(delivered = cancelledByPeer)
            }
        }
    }

    override suspend fun cancelTransfer(offer: IncomingFileOffer): Boolean {
        incomingAttempt(offer)
        return cancel()
    }

    private suspend fun cancel(): Boolean {
        val tx = active ?: return false
        finalReceipt.sending()
        try {
            control(tx.reply(PdfCommand.CANCEL))
            finalReceipt.finish(delivered = true)
        } catch (error: Exception) {
            finalReceipt.finish(delivered = false)
            throw error
        } finally {
            abort(tx, notify = false)
        }
        return true
    }

    override fun onPayloadReceived(endpoint: String, payload: Payload) {
        if (endpoint != peer.id || !authenticated()) {
            discard(payload)
            return
        }
        try {
            when (payload.type) {
                Payload.Type.BYTES -> handle(TransferFrame.decode(checkNotNull(payload.asBytes())))
                Payload.Type.FILE -> {
                    val tx = active
                    val item = tx?.byPayload(payload.id)
                    if (role != NearbyRole.Discoverer || item == null || item.file.isCompleted) {
                        discard(payload)
                        return
                    }
                    item.handle = payload
                    item.file.complete(payload)
                }

                else -> discard(payload)
            }
        } catch (_: Exception) {
            active?.let { abort(it, notify = true) }
        }
    }

    private fun handle(frame: TransferFrame) {
        if (frame.command == PdfCommand.OFFER) {
            check(role == NearbyRole.Discoverer && active == null)
            finalReceipt.reset()
            active = BatchAttempt(frame)
            pending.value = IncomingFileOffer(frame.transfer.toString(), peer.name, frame.entries.map { it.file })
            return
        }
        val tx = active ?: return
        if (tx.offer.transfer != frame.transfer || tx.offer.legacy != frame.legacy) return
        when (frame.command) {
            PdfCommand.ACCEPT, PdfCommand.DECLINE, PdfCommand.SKIP -> {
                check(role == NearbyRole.Advertiser && !tx.decision.isCompleted)
                tx.decision.complete(when (frame.command) {
                    PdfCommand.DECLINE -> FileOfferDecision.Declined
                    PdfCommand.SKIP -> FileOfferDecision.Accepted(emptySet())
                    else -> {
                        val ids = if (frame.legacy) listOf(tx.items.single().entry.file.id) else frame.ids
                        tx.approve(ids)
                        FileOfferDecision.Accepted(ids.toSet())
                    }
                })
            }
            PdfCommand.START -> {
                val item = tx.resolve(frame)
                check(role == NearbyRole.Discoverer && item === tx.approved.first { !it.saved.isCompleted })
                check(item.started.complete(Unit))
            }
            PdfCommand.SAVED -> {
                val item = tx.resolve(frame)
                check(role == NearbyRole.Advertiser && item.started.isCompleted)
                check(item.saved.complete(Unit))
            }

            PdfCommand.CANCEL -> cancelFromPeer(tx)
            PdfCommand.FAIL -> abort(tx, notify = false)
            PdfCommand.OFFER -> error("Already handled")
        }
    }

    override fun onPayloadTransferUpdate(endpoint: String, update: PayloadTransferUpdate) {
        if (endpoint != peer.id || !authenticated()) return
        val status = update.status
        controls[update.payloadId]?.let {
            when (status) {
                PayloadTransferUpdate.Status.SUCCESS -> it.complete(Unit)
                PayloadTransferUpdate.Status.FAILURE, PayloadTransferUpdate.Status.CANCELED ->
                    it.completeExceptionally(IOException("Control delivery failed"))
            }
            return
        }
        val tx = active ?: return
        val item = tx.byPayload(update.payloadId) ?: return
        if (item.saved.isCompleted && role == NearbyRole.Discoverer) return
        when (status) {
            PayloadTransferUpdate.Status.IN_PROGRESS -> {
                if (update.bytesTransferred > item.entry.file.sizeBytes) abort(tx, notify = true)
                else item.progress.trySend(update.bytesTransferred)
            }
            PayloadTransferUpdate.Status.SUCCESS -> {
                item.progress.trySend(update.bytesTransferred)
                item.progress.close()
            }
            PayloadTransferUpdate.Status.FAILURE, PayloadTransferUpdate.Status.CANCELED -> abort(tx, notify = true)
        }
    }

    private fun discard(payload: Payload) {
        client.cancelPayload(payload.id)
        if (payload.type == Payload.Type.FILE) removeTemporary(payload) else payload.close()
    }

    private fun removeTemporary(payload: Payload) {
        store.discardTemporary(payload)
    }

    private fun release(tx: BatchAttempt) {
        if (active !== tx) return
        active = null
        pending.value = null
        tx.items.forEach(::closeHandle)
    }

    private fun closeHandle(item: FileExchange) {
        val handle = item.handle ?: return
        item.handle = null
        if (role == NearbyRole.Discoverer) removeTemporary(handle) else handle.close()
    }

    private fun abort(tx: BatchAttempt, notify: Boolean) {
        if (active !== tx) return
        if (notify && authenticated()) {
            runCatching { send(Payload.fromBytes(tx.reply(PdfCommand.FAIL).encode())) {} }
        }
        tx.approved.filter { it.started.isCompleted && !it.saved.isCompleted }.forEach {
            client.cancelPayload(it.entry.payloadId)
        }
        val error = IOException("File exchange ended")
        release(tx)
        tx.decision.completeExceptionally(error)
        tx.items.forEach { it.end(error) }
    }

    private fun cancelFromPeer(tx: BatchAttempt) {
        if (active !== tx) return
        finalReceipt.sending()
        finalReceipt.finish(delivered = true)
        tx.approved.filter { it.started.isCompleted && !it.saved.isCompleted }.forEach {
            client.cancelPayload(it.entry.payloadId)
        }
        val error = PeerCancelledException()
        release(tx)
        tx.decision.completeExceptionally(error)
        tx.items.forEach { it.end(error) }
    }

    fun close() {
        active?.let { abort(it, notify = false) }
        controls.values.toList().forEach { it.completeExceptionally(IOException("Disconnected")) }
        controls.clear()
    }
}

private suspend fun <T : Any> CompletableDeferred<T>.waitFor(ms: Long): T =
    withTimeoutOrNull(ms) { await() } ?: throw IOException("Peer did not respond")
