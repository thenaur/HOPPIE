package com.example.hoppie.features.receive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoppie.core.components.FileTransferBackground
import com.example.hoppie.core.components.HoppieNotice
import com.example.hoppie.core.components.HoppieNotificationCard
import com.example.hoppie.core.components.TransferHeader
import com.example.hoppie.core.theme.HOPPIETheme
import com.example.hoppie.data.nearby.NearbyRole
import com.example.hoppie.data.nearby.Phase
import com.example.hoppie.data.nearby.SessionState
import com.example.hoppie.features.receive.components.nearbyDevicesSection
import com.example.hoppie.features.receive.components.ReceiveDiscoveryStatus
import com.example.hoppie.features.receive.components.ReceiveMascot
import com.example.hoppie.features.receive.components.RescanButton
import com.example.hoppie.features.send.FileTransferState
import com.example.hoppie.features.send.components.NearbyConnectionDialog
import com.example.hoppie.features.send.TransferStatus
import com.example.hoppie.features.send.TransferActionButton
import com.example.hoppie.features.send.TransferringFilesScreen

@Composable
internal fun ReceiveScreen(
    onBack: () -> Unit,
    model: ReceiveViewModel = viewModel(),
    onTransferDone: () -> Unit = onBack,
    nearby: SessionState? = null,
    onScan: (() -> Unit)? = null,
    onConnect: ((String) -> Unit)? = null,
    onStop: () -> Unit = {},
    onDecision: (SessionState, Boolean) -> Unit = { _, _ -> },
    onOpenTransfers: (() -> Unit)? = null,
    onDismissNotice: () -> Unit = {}
) {
    val state by model.state.collectAsStateWithLifecycle()
    val dismissAllNotices = {
        onDismissNotice()
        model.clearNotice()
    }
    val transfer = state.transfer
    val incoming = state.incoming
    var exitingCompletion by remember { mutableStateOf<Pair<FileTransferState, String>?>(null) }
    var entered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!entered) {
            entered = true
            if (incoming == null && transfer == null) onScan?.invoke()
        }
    }
    val sessionAction = when (nearby?.phase) {
        null, Phase.Idle -> null
        Phase.Connected -> "Disconnect"
        else -> "Stop"
    }
    if (nearby?.phase == Phase.Verify) NearbyConnectionDialog(nearby, onDecision)
    val completedSnapshot = transfer
        ?.takeIf { !it.active && it.verifiedCount > 0 && it.status == TransferStatus.Completed }
        ?.let { it to incoming?.offer?.senderName.orEmpty() }
        ?: exitingCompletion.takeIf { transfer == null }
    when {
        completedSnapshot != null -> {
            val (completedTransfer, senderName) = completedSnapshot
            ReceivedFilesSummary(completedTransfer, senderName) {
                if (exitingCompletion == null && model.closeTransfer()) {
                    exitingCompletion = completedSnapshot
                    dismissAllNotices()
                    if (nearby?.phase == Phase.Connected) onStop()
                    onTransferDone()
                }
            }
        }
        transfer != null && !transfer.active && transfer.verifiedCount > 0 ->
            ReceivedFilesSummary(transfer, incoming?.offer?.senderName.orEmpty()) {
                model.closeTransfer()
                if (nearby?.phase == Phase.Connected) onStop()
            }
        transfer != null -> TransferringFilesScreen(
            transfer,
            model::cancelTransfer,
            onDone = {
                val wasActive = transfer.active
                if (model.closeTransfer() && !wasActive && nearby?.phase == Phase.Connected) {
                    onStop()
                }
            },
            exitLabel = "Back to Receive Files",
            onOpenTransfers = onOpenTransfers
        )
        incoming?.reviewingDuplicates == true -> {
            val file = requireNotNull(incoming.duplicateReview).remaining.first()
            DuplicateFilesScreen(
                file = file,
                busy = incoming.busy,
                error = incoming.error,
                onBack = model::returnToIncomingFiles,
                onSkip = { model.decideDuplicate(file.id, skip = true) },
                onCopy = { model.decideDuplicate(file.id, skip = false) }
            )
        }
        incoming != null -> IncomingFilesScreen(incoming, model::acceptFiles, model::declineFiles)
        else -> {
            val display = if (nearby == null) state else {
                val discovery = receiverDiscoveryStatus(nearby)
                val noticeCard = if (discovery == DiscoveryStatus.Cancelled) null
                else nearby.cardNotice ?: state.cardNotice ?: nearby.notice?.let { HoppieNotice.Info(it) }
                state.copy(
                    discovery = discovery,
                    devices = receiverDevices(nearby),
                    notice = nearby.notice?.takeUnless { discovery == DiscoveryStatus.Cancelled } ?: state.notice,
                    cardNotice = noticeCard
                )
            }
            ReceiveContent(
                display,
                onBack = {
                    dismissAllNotices()
                    onBack()
                },
                onRescan = onScan?.let { scan ->
                    {
                        dismissAllNotices()
                        scan()
                    }
                },
                onConnect = onConnect.takeIf { nearby?.phase == Phase.Searching }?.let { connect ->
                    { id ->
                        dismissAllNotices()
                        connect(id)
                    }
                },
                sessionAction = sessionAction,
                onStop = onStop,
                onDismissNotice = dismissAllNotices
            )
        }
    }
}

internal fun receiverDiscoveryStatus(nearby: SessionState): DiscoveryStatus = when {
    nearby.phase == Phase.Idle && nearby.connectionCancelled -> DiscoveryStatus.Cancelled
    nearby.phase == Phase.Idle -> DiscoveryStatus.Completed
    nearby.role != NearbyRole.Discoverer -> DiscoveryStatus.SessionActive
    nearby.phase == Phase.Starting -> DiscoveryStatus.Starting
    nearby.phase == Phase.Searching -> DiscoveryStatus.Scanning
    nearby.phase == Phase.Requesting -> DiscoveryStatus.Connecting(nearby.peer?.name.orEmpty())
    nearby.phase == Phase.Verify -> DiscoveryStatus.Verifying(nearby.peer?.name.orEmpty())
    nearby.phase == Phase.AwaitingConnectionResult -> DiscoveryStatus.AwaitingConfirmation(nearby.peer?.name.orEmpty())
    nearby.phase == Phase.Declining -> DiscoveryStatus.Declining
    nearby.phase == Phase.Connected -> DiscoveryStatus.Connected(nearby.peer?.name.orEmpty())
    else -> DiscoveryStatus.SessionActive
}

internal fun receiverDevices(nearby: SessionState): List<NearbyDevice> {
    if (nearby.role != NearbyRole.Discoverer) return emptyList()
    val activePeer = nearby.peer
    val peers = if (activePeer != null &&
        nearby.phase in setOf(Phase.Requesting, Phase.Verify, Phase.AwaitingConnectionResult, Phase.Connected) &&
        activePeer.id !in nearby.endpoints
    ) {
        nearby.endpoints.values + activePeer
    } else {
        nearby.endpoints.values
    }
    return peers.map { peer ->
        val connection = when {
            peer.id != activePeer?.id -> DeviceConnection.Available
            nearby.phase == Phase.Connected -> DeviceConnection.Connected
            nearby.phase in setOf(Phase.Requesting, Phase.Verify, Phase.AwaitingConnectionResult) -> DeviceConnection.Connecting
            else -> DeviceConnection.Available
        }
        NearbyDevice(peer.id, peer.name, connection)
    }
}

@Composable
internal fun ReceiveContent(
    state: ReceiveUiState,
    onBack: () -> Unit,
    onRescan: (() -> Unit)? = null,
    onConnect: ((String) -> Unit)? = null,
    sessionAction: String? = null,
    onStop: () -> Unit = {},
    onDismissNotice: () -> Unit = {}
) {
    val activeNotice = state.cardNotice ?: state.notice?.let { HoppieNotice.Info(it) }
    FileTransferBackground {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
            TransferHeader(onBack, title = "Receive Files", modifier = Modifier.padding(top = 8.dp))
            activeNotice?.let { notice ->
                HoppieNotificationCard(
                    notice = notice,
                    onDismiss = onDismissNotice,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "mascot") { ReceiveMascot() }
                item(key = "discovery") {
                    ReceiveDiscoveryStatus(state.discovery, Modifier.padding(top = 8.dp, bottom = 12.dp))
                }
                nearbyDevicesSection(state.devices, onConnect)
            }
            if (sessionAction != null) TransferActionButton(sessionAction, onClick = onStop)
            else RescanButton(
                onRescan = onRescan,
                enabled = state.discovery != DiscoveryStatus.Starting,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 820)
@Composable
private fun ReceiveDevicesPreview() {
    HOPPIETheme {
        ReceiveContent(
            state = ReceiveUiState(
                discovery = DiscoveryStatus.Scanning,
                devices = listOf(
                    NearbyDevice("preview-1", "Example Phone", DeviceConnection.Connecting),
                    NearbyDevice("preview-2", "Example Tablet"),
                    NearbyDevice("preview-3", "Example Laptop", DeviceConnection.Connected),
                    NearbyDevice("preview-4", "Example Device", DeviceConnection.Failed("Could not connect. Try again."))
                )
            ),
            onBack = {},
            onRescan = {},
            onConnect = {}
        )
    }
}
