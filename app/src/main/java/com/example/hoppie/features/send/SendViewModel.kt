package com.example.hoppie.features.send

import android.app.Application
import android.net.Uri
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoppie.core.components.FileOfferDecision
import com.example.hoppie.core.components.HoppieNotice
import com.example.hoppie.core.components.OfferedFile
import com.example.hoppie.core.components.sha256
import com.example.hoppie.data.nearby.TransferFrame
import com.example.hoppie.features.transfers.CompletedTransferHistory
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SendViewModel(application: Application) : AndroidViewModel(application) {
    private val resolver = application.contentResolver
    private val _state = MutableStateFlow(SendUiState())
    val state = _state.asStateFlow()

    private var connectionActions: IncomingConnectionActions? = null
    private var connectionJob: Job? = null
    private var transferJob: Job? = null
    private var cancelPayloads: (suspend () -> Boolean)? = null

    @MainThread
    fun onIncomingConnectionRequest(
        request: IncomingConnectionRequest,
        actions: IncomingConnectionActions
    ): Boolean {
        // The transport owns duplicate delivery and rejection of additional endpoints.
        val current = _state.value
        if (current.incomingConnection != null || current.transfer != null ||
            current.files.isEmpty() || current.isCheckingFiles || current.waitingForReceiver
        ) return false
        connectionActions = actions
        _state.update { it.copy(incomingConnection = IncomingConnectionUiState(request)) }
        return true
    }

    @MainThread
    fun acceptIncomingConnection() = respondToConnection(accept = true)

    @MainThread
    fun declineIncomingConnection() = respondToConnection(accept = false)

    @MainThread
    fun onIncomingConnectionEnded(request: IncomingConnectionRequest, timedOut: Boolean) {
        if (_state.value.incomingConnection?.request !== request) return
        connectionJob?.cancel()
        clearIncomingConnection(
            if (timedOut) "The connection request timed out. Ask the device to try again."
            else "The connection request was cancelled."
        )
    }

    private fun respondToConnection(accept: Boolean) {
        val incoming = _state.value.incomingConnection ?: return
        val actions = connectionActions ?: return
        if (incoming.status != IncomingConnectionStatus.AwaitingDecision) return
        val busy = if (accept) IncomingConnectionStatus.Accepting else IncomingConnectionStatus.Declining
        _state.update { it.copy(incomingConnection = incoming.copy(status = busy, error = null)) }

        connectionJob = viewModelScope.launch {
            val result = try {
                if (accept) actions.accept(incoming.request.endpointId)
                else actions.decline(incoming.request.endpointId)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                ConnectionResponse.Failed(requestStillPending = false)
            }
            currentCoroutineContext().ensureActive()
            if (_state.value.incomingConnection?.request !== incoming.request) return@launch

            when (result) {
                ConnectionResponse.Completed -> {
                    if (accept) {
                        connectionActions = null
                        awaitReceiverDecision(incoming.request.endpointId, incoming.request.deviceName, actions)
                    } else {
                        clearIncomingConnection()
                    }
                }
                is ConnectionResponse.Failed -> {
                    val action = if (accept) "connect" else "decline the request"
                    if (result.requestStillPending) {
                        _state.update {
                            it.copy(incomingConnection = incoming.copy(error = "Could not $action. Try again."))
                        }
                    } else {
                        clearIncomingConnection("Could not $action. Ask the device to try again.")
                    }
                }
            }
        }
    }

    private var offeredGeneration: Long? = null

    fun offerConnectedFiles(generation: Long, endpointId: String, recipientName: String, actionsFor: (List<SelectedFile>) -> OutgoingFilesActions) {
        val current = _state.value
        if (offeredGeneration == generation) return
        if (current.isCheckingFiles || current.waitingForReceiver || current.transfer != null) return
        offeredGeneration = generation
        val files = current.files
        if (files.size !in 1..TransferFrame.MAX_FILES) {
            _state.update {
                it.copy(connectionNotice = HoppieNotice.Info("Too many files", "Select between 1 and ${TransferFrame.MAX_FILES} files."))
            }
            return
        }
        connectionJob = viewModelScope.launch { awaitReceiverDecision(endpointId, recipientName, actionsFor(files)) }
    }

    private suspend fun awaitReceiverDecision(endpointId: String, recipientName: String, actions: OutgoingFilesActions) {
        val files = _state.value.files
        val filesById = files.withIndex().associate { (index, file) -> index.toString() to file }
        _state.update { it.copy(incomingConnection = null, waitingForReceiver = true, connectionNotice = null) }
        val decision = try {
            val manifest = filesById.map { (id, file) ->
                val digest = sha256 {
                    resolver.openInputStream(file.uri) ?: throw IOException("Could not read the selected file")
                }
                OfferedFile(id, file.name, file.sizeBytes, file.mimeType, digest)
            }
            actions.offerFiles(endpointId, manifest)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            FileOfferDecision.Ended
        }
        currentCoroutineContext().ensureActive()
        when (decision) {
            is FileOfferDecision.Accepted -> when {
                decision.fileIds.isEmpty() -> {
                    endFileOffer(HoppieNotice.Info(recipientName.ifBlank { "Nearby device" }, "skipped all files."))
                    actions.finishSession()
                }
                !filesById.keys.containsAll(decision.fileIds) -> {
                    endFileOffer(HoppieNotice.Info("Invalid selection", "The receiver's file selection was invalid."))
                    actions.finishSession()
                }
                else -> beginTransfer(endpointId, recipientName, actions, filesById.filterKeys { it in decision.fileIds })
            }
            FileOfferDecision.Declined -> {
                endFileOffer(HoppieNotice.FilesDeclined(recipientName))
                actions.finishSession()
            }
            FileOfferDecision.Ended -> {
                endFileOffer(HoppieNotice.ConnectionLost())
                actions.finishSession()
            }
        }
    }

    fun stopWaitingForReceiver() {
        if (!_state.value.waitingForReceiver) return
        connectionJob?.cancel()
        endFileOffer()
    }

    private fun endFileOffer(notice: HoppieNotice? = null) {
        _state.update { it.copy(waitingForReceiver = false, connectionNotice = notice) }
    }

    private fun beginTransfer(
        endpointId: String,
        recipientName: String,
        actions: OutgoingFilesActions,
        files: Map<String, SelectedFile>
    ) {
        cancelPayloads = { actions.cancelTransfer(endpointId) }
        _state.update {
            it.copy(
                incomingConnection = null,
                connectionNotice = null,
                waitingForReceiver = false,
                transfer = FileTransferState(files.values.map { file -> TransferFile(file) }, recipientName = recipientName)
            )
        }
        transferJob = viewModelScope.launch {
            try {
                actions.transfer(endpointId, files)
                    .takeWhile { event ->
                        updateTransfer { it.applyEvent(event) }
                        event != FileTransferEvent.Cancelled
                    }
                    .collect()
                finishTransfer(actions)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateTransfer { if (it.active) transferFailed(it) else it }
            }
        }
    }

    private fun finishTransfer(actions: OutgoingFilesActions) {
        val transfer = _state.value.transfer ?: return
        if (!transfer.active) return
        if (transfer.files.isEmpty() || transfer.files.any { !it.verified }) {
            updateTransfer(::transferFailed)
            return
        }
        updateTransfer { it.copy(status = TransferStatus.Completed, cancelling = false, message = null) }
        actions.finishSession()
    }

    fun finishCompletedTransfer(): Boolean {
        if (_state.value.transfer?.status != TransferStatus.Completed) return false
        cancelPayloads = null
        _state.value = SendUiState()
        return true
    }

    fun cancelTransfer() {
        val cancel = cancelPayloads ?: return
        val transfer = _state.value.transfer ?: return
        if (!transfer.active || transfer.cancelling) return
        val job = transferJob ?: return
        updateTransfer { it.copy(cancelling = true, message = null) }
        viewModelScope.launch {
            val cancelled = try {
                cancel()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                false
            }
            if (transferJob !== job || _state.value.transfer?.active != true) return@launch
            if (cancelled) {
                updateTransfer { it.copy(status = TransferStatus.Cancelled, cancelling = false) }
                job.cancel()
            } else {
                updateTransfer { it.copy(cancelling = false, message = "Could not cancel. Try again.") }
            }
        }
    }

    fun closeTransfer() {
        if (_state.value.transfer?.active != false) return
        cancelPayloads = null
        _state.update { it.copy(transfer = null) }
    }

    fun resetAbandonedSession() {
        if (_state.value.transfer.isPayloadTransferActive) return
        stopWaitingForReceiver()
        connectionJob?.cancel()
        clearIncomingConnection()
        when (_state.value.transfer?.status) {
            TransferStatus.Completed -> finishCompletedTransfer()
            TransferStatus.Failed, TransferStatus.Cancelled -> closeTransfer()
            else -> Unit
        }
    }

    private fun updateTransfer(update: (FileTransferState) -> FileTransferState) {
        val before = _state.value.transfer ?: return
        val after = update(before)
        _state.update { it.copy(transfer = after) }
        if (before.active && !after.active) CompletedTransferHistory(getApplication()).record(after, received = false)
    }

    private fun transferFailed(transfer: FileTransferState) = transfer.copy(
        status = TransferStatus.Failed,
        cancelling = false,
        message = "The transfer could not be verified. Return to Send Files to try again."
    )

    private fun clearIncomingConnection(message: String? = null) {
        connectionActions = null
        val notice = message?.let { HoppieNotice.Info("Connection request ended", it) }
        _state.update { it.copy(incomingConnection = null, connectionNotice = notice) }
    }

    fun clearConnectionNotice() {
        if (_state.value.connectionNotice != null) {
            _state.update { it.copy(connectionNotice = null) }
        }
    }

    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty() || _state.value.isCheckingFiles) return
        val existingUris = _state.value.files.mapTo(mutableSetOf()) { it.uri }
        val candidates = uris.distinct().filterNot(existingUris::contains)
        if (candidates.isEmpty()) {
            _state.update { it.copy(validationMessages = emptyList()) }
            return
        }

        // OpenMultipleDocuments grants read access for this Activity's task.
        _state.update { it.copy(isCheckingFiles = true, validationMessages = emptyList()) }
        viewModelScope.launch {
            val (accepted, messages) = withContext(Dispatchers.IO) {
                val files = mutableListOf<SelectedFile>()
                val rejected = mutableListOf<String>()
                for (uri in candidates) {
                    currentCoroutineContext().ensureActive()
                    when (val result = readSelectedFile(resolver, uri)) {
                        is FileCheck.Accepted -> files.add(result.file)
                        is FileCheck.Rejected -> rejected.add(result.message)
                    }
                }
                files to rejected
            }
            _state.update {
                it.copy(
                    files = it.files + accepted,
                    validationMessages = messages,
                    isCheckingFiles = false
                )
            }
        }
    }

    fun removeFile(uri: Uri) {
        _state.update { state -> state.copy(files = state.files.filterNot { it.uri == uri }) }
    }
}
