package com.example.hoppie.features.receive

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoppie.core.components.HoppieNotice
import com.example.hoppie.features.send.FileTransferEvent
import com.example.hoppie.features.send.FileTransferState
import com.example.hoppie.features.send.MAX_FILE_BYTES
import com.example.hoppie.features.send.TransferFile
import com.example.hoppie.features.send.TransferStatus
import com.example.hoppie.features.send.isPayloadTransferActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface IncomingFilesActions {
    /** List destination names and the digests of stored content that can match this offer. */
    suspend fun inspectDestination(offer: IncomingFileOffer): List<StoredFile>

    /**
     * Reserve destinations without overwriting, send only the selected IDs to the sender, and
     * return after acknowledgment. An empty map acknowledges skipping every file and sends no payloads.
     * Report payload completion only after verifying and saving the new copy.
     */
    suspend fun accept(offer: IncomingFileOffer, destinations: Map<String, String>): Flow<FileTransferEvent>

    /** Return only after rejection is handled. Throw if the still-pending offer can be retried. */
    suspend fun decline(offer: IncomingFileOffer)

    suspend fun cancelTransfer(offer: IncomingFileOffer): Boolean
}

internal class ReceiveViewModel(private val record: (FileTransferState, String?) -> Unit = { _, _ -> }) : ViewModel() {
    private val _state = MutableStateFlow(
        ReceiveUiState(DiscoveryStatus.Unavailable("Nearby device discovery is not available in this version."))
    )
    val state = _state.asStateFlow()

    private var actions: IncomingFilesActions? = null
    private var sessionJob: Job? = null

    /** Called by the adapter after connection confirmation and manifest receipt from that endpoint. */
    @MainThread
    fun onIncomingFileOffer(offer: IncomingFileOffer, actions: IncomingFilesActions): Boolean {
        val current = _state.value
        if (current.incoming != null || offer.sessionId.isBlank() || offer.senderName.isBlank() ||
            offer.files.isEmpty() || offer.files.any {
                it.id.isBlank() || it.name.isBlank() || it.name == "." || it.name == ".." ||
                    '/' in it.name || '\\' in it.name || '\u0000' in it.name || it.sizeBytes !in 0..MAX_FILE_BYTES
            } || offer.files.distinctBy { it.id }.size != offer.files.size
        ) return false
        this.actions = actions
        _state.update {
            it.copy(
                incoming = IncomingFilesState(offer.copy(files = offer.files.toList())),
                notice = null,
                cardNotice = null
            )
        }
        return true
    }

    @MainThread
    fun onIncomingOfferEnded(sessionId: String, message: String) {
        if (_state.value.incoming?.offer?.sessionId != sessionId) return
        sessionJob?.cancel()
        val transfer = _state.value.transfer
        if (transfer?.active == true) {
            updateTransfer { it.copy(status = TransferStatus.Failed, cancelling = false, message = message) }
        } else if (transfer == null) {
            val card = when (message) {
                "Disconnected.", "Connection lost." -> HoppieNotice.ConnectionLost()
                else -> HoppieNotice.Info("File offer ended", message)
            }
            clearOffer(message, card)
        }
    }

    fun acceptFiles() {
        val incoming = _state.value.incoming ?: return
        if (incoming.busy) return
        if (incoming.duplicateReview != null) {
            _state.update { it.copy(incoming = incoming.copy(reviewingDuplicates = true, error = null)) }
            return
        }
        respond(incoming, accepting = true) { protocol ->
            val review = withContext(Dispatchers.IO) {
                reviewDuplicates(incoming.offer.files, protocol.inspectDestination(incoming.offer))
            }
            if (review.remaining.isEmpty()) {
                receiveApproved(incoming.offer, protocol, review)
            } else {
                _state.update {
                    it.copy(incoming = incoming.copy(duplicateReview = review, reviewingDuplicates = true))
                }
            }
        }
    }

    fun declineFiles() {
        val incoming = _state.value.incoming ?: return
        respond(incoming, accepting = false) { protocol ->
            protocol.decline(incoming.offer)
            currentCoroutineContext().ensureActive()
            clearOffer("Files declined.", HoppieNotice.Info("Files declined", "You declined the incoming files."))
        }
    }

    fun returnToIncomingFiles() {
        val incoming = _state.value.incoming ?: return
        if (!incoming.busy) {
            _state.update { it.copy(incoming = incoming.copy(reviewingDuplicates = false, error = null)) }
        }
    }

    fun decideDuplicate(fileId: String, skip: Boolean) {
        val incoming = _state.value.incoming ?: return
        val review = incoming.duplicateReview ?: return
        if (incoming.busy || !incoming.reviewingDuplicates || review.remaining.firstOrNull()?.id != fileId) return
        val next = review.decide(skip)
        if (next.remaining.isEmpty()) {
            respond(incoming, accepting = true) { protocol -> receiveApproved(incoming.offer, protocol, next) }
        } else {
            _state.update { it.copy(incoming = incoming.copy(duplicateReview = next, error = null)) }
        }
    }

    private fun respond(
        incoming: IncomingFilesState,
        accepting: Boolean,
        action: suspend (IncomingFilesActions) -> Unit
    ) {
        val protocol = actions ?: return
        if (incoming.busy || _state.value.transfer != null) return
        _state.update { it.copy(incoming = incoming.copy(busy = true, accepting = accepting, error = null)) }
        sessionJob = viewModelScope.launch {
            try {
                action(protocol)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (_state.value.transfer == null) {
                    _state.update {
                        it.copy(incoming = incoming.copy(error = "Could not prepare your files. Try again."))
                    }
                } else {
                    updateTransfer { if (it.active) failedTransfer(it) else it }
                }
            }
        }
    }

    private suspend fun receiveApproved(
        offer: IncomingFileOffer,
        protocol: IncomingFilesActions,
        review: DuplicateFileReview
    ) {
        val destinations = review.destinations(offer.files)
        val events = protocol.accept(offer, destinations)
        currentCoroutineContext().ensureActive()
        if (destinations.isEmpty()) {
            clearOffer("All files were skipped.", HoppieNotice.Info("Files skipped", "All files were skipped."))
            return
        }
        val files = offer.files.mapNotNull { file ->
            destinations[file.id]?.let { name -> TransferFile(file.copy(name = name).asTransferFile(), originalName = file.name) }
        }
        _state.update { it.copy(transfer = FileTransferState(files)) }
        events.takeWhile { event ->
            updateTransfer { it.applyEvent(event) }
            event != FileTransferEvent.Cancelled
        }.collect()
        updateTransfer { transfer ->
            when {
                !transfer.active -> transfer
                transfer.files.all { it.verified } -> transfer.copy(
                    status = TransferStatus.Completed, cancelling = false, message = null
                )
                else -> failedTransfer(transfer)
            }
        }
    }

    fun cancelTransfer() {
        val incoming = _state.value.incoming ?: return
        val protocol = actions ?: return
        val transfer = _state.value.transfer ?: return
        if (!transfer.active || transfer.cancelling) return
        val job = sessionJob
        updateTransfer { it.copy(cancelling = true, message = null) }
        viewModelScope.launch {
            val stopped = try {
                protocol.cancelTransfer(incoming.offer)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                false
            }
            if (sessionJob !== job || _state.value.transfer?.active != true) return@launch
            if (stopped) {
                updateTransfer { it.copy(status = TransferStatus.Cancelled, cancelling = false) }
                job?.cancel()
            } else {
                updateTransfer { it.copy(cancelling = false, message = "Could not cancel. Try again.") }
            }
        }
    }

    fun closeTransfer(): Boolean {
        if (_state.value.transfer?.active != false) return false
        clearOffer()
        return true
    }

    fun resetAbandonedSession() {
        if (_state.value.transfer.isPayloadTransferActive) return
        val incoming = _state.value.incoming
        if (incoming != null) {
            sessionJob?.cancel()
            clearOffer()
            return
        }
        if (_state.value.transfer?.active == false) {
            clearOffer()
        } else if (_state.value.notice != null || _state.value.cardNotice != null) {
            _state.update { it.copy(notice = null, cardNotice = null) }
        }
    }

    fun clearNotice() {
        if (_state.value.notice != null || _state.value.cardNotice != null) {
            _state.update { it.copy(notice = null, cardNotice = null) }
        }
    }

    private fun clearOffer(notice: String? = null, cardNotice: HoppieNotice? = null) {
        actions = null
        _state.update { it.copy(incoming = null, transfer = null, notice = notice, cardNotice = cardNotice) }
    }

    private fun updateTransfer(update: (FileTransferState) -> FileTransferState) {
        val before = _state.value.transfer ?: return
        val after = update(before)
        _state.update { it.copy(transfer = after) }
        if (before.active && !after.active) record(after, _state.value.incoming?.offer?.senderName)
    }

    private fun failedTransfer(transfer: FileTransferState) = transfer.copy(
        status = TransferStatus.Failed,
        cancelling = false,
        message = "The incoming files could not be verified. Ask the sender to try again."
    )
}
