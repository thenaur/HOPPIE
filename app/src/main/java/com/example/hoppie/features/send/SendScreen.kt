package com.example.hoppie.features.send

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoppie.core.components.FileTransferBackground
import com.example.hoppie.core.components.HoppieNotice
import com.example.hoppie.core.components.HoppieNotificationCard
import com.example.hoppie.core.components.TransferHeader
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.data.nearby.Phase
import com.example.hoppie.data.nearby.SessionState
import com.example.hoppie.features.send.components.IncomingConnectionDialog
import com.example.hoppie.features.send.components.NearbyConnectionDialog
import com.example.hoppie.features.send.components.SendButton
import com.example.hoppie.features.send.components.SelectedFilesSummary
import com.example.hoppie.features.send.components.selectedFilesSection
import com.example.hoppie.features.send.components.UploadFilesCard

@Composable
internal fun SendScreen(
    onBack: () -> Unit,
    model: SendViewModel = viewModel(),
    onTransferDone: () -> Unit = onBack,
    onAdvertise: (() -> Unit)? = null,
    session: SessionState? = null,
    onStopSharing: () -> Unit = {},
    onDecision: (SessionState, Boolean) -> Unit = { _, _ -> },
    onOpenTransfers: (() -> Unit)? = null,
    idleNotice: HoppieNotice? = null,
    onDismissNotice: () -> Unit = {}
) {
    val state by model.state.collectAsStateWithLifecycle()
    val dismissAllNotices = {
        onDismissNotice()
        model.clearConnectionNotice()
    }

    var exitingCompletion by remember { mutableStateOf<FileTransferState?>(null) }
    val activeOrCompleted = state.transfer
    val completedSnapshot = activeOrCompleted?.takeIf { it.status == TransferStatus.Completed }
        ?: exitingCompletion.takeIf { activeOrCompleted == null }

    if (completedSnapshot != null) {
        TransferringFilesScreen(
            completedSnapshot,
            onCancel = model::cancelTransfer,
            onDone = {
                if (exitingCompletion == null && model.finishCompletedTransfer()) {
                    exitingCompletion = completedSnapshot
                    dismissAllNotices()
                    onTransferDone()
                }
            },
            onOpenTransfers = onOpenTransfers
        )
        return
    }

    activeOrCompleted?.let { transfer ->
        TransferringFilesScreen(transfer, model::cancelTransfer, {
            model.closeTransfer()
            onStopSharing()
        }, onOpenTransfers = onOpenTransfers)
        return
    }

    if (session != null) {
        BackHandler(onBack = onStopSharing)
        SendingFilesScreen(state, session, onStopSharing)
        if (session.phase == Phase.Verify) NearbyConnectionDialog(session, onDecision)
        return
    }

    if (state.waitingForReceiver) {
        AlertDialog(
            onDismissRequest = model::stopWaitingForReceiver,
            title = { Text("Waiting for receiver") },
            text = { Text("The receiver needs to review and accept the files before sharing begins.") },
            confirmButton = {
                TextButton(onClick = model::stopWaitingForReceiver) { Text("Stop waiting", color = HoppiePurple) }
            },
            containerColor = Color.White,
            titleContentColor = HoppiePurple,
            textContentColor = HoppiePurple
        )
    }

    state.incomingConnection?.let { incoming ->
        IncomingConnectionDialog(incoming, model::acceptIncomingConnection, model::declineIncomingConnection)
    }

    val activeNotice = idleNotice ?: state.connectionNotice

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White, contentColor = HoppiePurple) {
        FileTransferBackground {
            Column(modifier = Modifier.widthIn(max = 560.dp).fillMaxSize()) {
                TransferHeader(
                    onBack = {
                        dismissAllNotices()
                        onBack()
                    },
                    title = "Send Files",
                    modifier = Modifier.padding(top = 8.dp)
                )
                activeNotice?.let { notice ->
                    HoppieNotificationCard(
                        notice = notice,
                        onDismiss = dismissAllNotices,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    )
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = SendContentArrangement
                ) {
                    item(key = "upload") {
                        UploadFilesCard(
                            enabled = !state.isCheckingFiles,
                            onFilesPicked = model::addFiles,
                            modifier = Modifier.padding(horizontal = 24.dp)
                                .padding(top = if (activeNotice == null) 28.dp else 16.dp, bottom = 24.dp)
                        )
                    }
                    selectedFilesSection(state, model::removeFile)
                    item(key = "actions") {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SelectedFilesSummary(state.files.size, state.totalBytes)
                            SendButton(enabled = state.files.isNotEmpty() && !state.isCheckingFiles && onAdvertise != null) {
                                dismissAllNotices()
                                onAdvertise?.invoke()
                            }
                        }
                    }
                }
            }
        }
    }
}

// The list only arranges items when they all fit. Otherwise the footer scrolls with the files.
private object SendContentArrangement : Arrangement.Vertical {
    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        with(Arrangement.Top) { arrange(totalSize, sizes, outPositions) }
        if (sizes.isNotEmpty()) {
            val last = sizes.lastIndex
            outPositions[last] = maxOf(outPositions[last], totalSize - sizes[last])
        }
    }
}
