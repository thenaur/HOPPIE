package com.example.hoppie.features.transfers

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.home.components.HomeHeader
import com.example.hoppie.features.send.FileTransferState
import com.example.hoppie.features.send.formatFileSize
import com.example.hoppie.features.send.isPayloadTransferActive
import com.example.hoppie.features.transfers.components.OngoingTransferCard
import com.example.hoppie.features.transfers.components.TransferRecordCard
import com.example.hoppie.features.transfers.components.TransferStatusFilters

@Composable
internal fun TransfersScreen(
    sending: FileTransferState?,
    receiving: FileTransferState?,
    focusOngoingKey: Int = 0,
    onOpenActive: (received: Boolean) -> Unit
) {
    val context = LocalContext.current
    val history = remember(context) { CompletedTransferHistory(context).observe() }
    val records by history.collectAsStateWithLifecycle(initialValue = null)
    TransfersContent(records, sending, receiving, focusOngoingKey, onOpenActive)
}

@Composable
internal fun TransfersContent(
    records: List<CompletedTransfer>?,
    sending: FileTransferState?,
    receiving: FileTransferState?,
    focusOngoingKey: Int = 0,
    onOpenActive: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val sendingActive = sending.isPayloadTransferActive
    val receivingActive = receiving.isPayloadTransferActive
    val hasActive = sendingActive || receivingActive
    var filter by rememberSaveable {
        mutableStateOf(if (hasActive) TransferStatusFilter.Ongoing else TransferStatusFilter.Completed)
    }
    LaunchedEffect(hasActive, focusOngoingKey) {
        if (hasActive) filter = TransferStatusFilter.Ongoing
    }
    var openedId by rememberSaveable { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.TopCenter) {
        key(filter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    HomeHeader { Toast.makeText(context, "Notifications coming soon", Toast.LENGTH_SHORT).show() }
                }
                item { SectionHeading("Transfers") }
                item { TransferStatusFilters(filter) { filter = it } }
                if (filter == TransferStatusFilter.Ongoing) {
                    if (sendingActive && sending != null) item { OngoingTransferCard(sending, false) { onOpenActive(false) } }
                    if (receivingActive && receiving != null) item { OngoingTransferCard(receiving, true) { onOpenActive(true) } }
                    if (!hasActive) item { EmptyState(filter.emptyMessage) }
                } else {
                    item { SectionHeading("History") }
                    val filtered = records?.filter { it.completed == (filter == TransferStatusFilter.Completed) }
                    if (filtered.isNullOrEmpty()) {
                        item { EmptyState(if (records == null) "Loading transfer history…" else filter.emptyMessage) }
                    } else {
                        items(filtered, key = { it.id }) { record ->
                            TransferRecordCard(record, transferDate(record)) { openedId = record.id }
                        }
                    }
                }
            }
        }
    }
    records?.firstOrNull { it.id == openedId }?.let { record ->
        HistoryDetails(record) { openedId = null }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(title, Modifier.padding(top = 6.dp).semantics { heading() },
        style = MaterialTheme.typography.titleLarge, color = HoppiePurple)
}

@Composable
private fun EmptyState(message: String) {
    Text(message, Modifier.fillMaxWidth().padding(vertical = 16.dp),
        style = MaterialTheme.typography.bodyLarge, color = HoppieFileSize)
}

@Composable
private fun transferDate(record: CompletedTransfer): String = DateUtils.formatDateTime(
    LocalContext.current, record.completedAtMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
)

@Composable
private fun HistoryDetails(record: CompletedTransfer, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose, containerColor = Color.White, titleContentColor = HoppiePurple,
        title = { Text("Transfer details") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(record.summaryTitle, style = MaterialTheme.typography.titleMedium, color = HoppiePurple)
                    Text(record.summaryLine, color = HoppieFileSize)
                    Text(transferDate(record), color = HoppieFileSize)
                }
                items(record.files) { file ->
                    Text(file.name, color = HoppiePurple, style = MaterialTheme.typography.bodyLarge)
                    Text(formatFileSize(file.sizeBytes) + if (file.verified) " · Verified" else " · Not completed",
                        color = HoppieFileSize, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close", color = HoppiePurple) } }
    )
}

internal fun fileCountLabel(count: Int) = "$count " + if (count == 1) "file" else "files"

internal val CompletedTransfer.summaryTitle: String
    get() {
        val direction = if (completed) {
            if (received) "Received" else "Sent"
        } else {
            if (received) "Receive failed ·" else "Send failed ·"
        }
        return "$direction ${fileCountLabel(files.size)}"
    }

internal val CompletedTransfer.summaryLine: String
    get() {
        val direction = if (received) "from" else "to"
        return "${formatFileSize(files.sumOf { it.sizeBytes })} · $direction ${peerName ?: "Unknown device"}"
    }
