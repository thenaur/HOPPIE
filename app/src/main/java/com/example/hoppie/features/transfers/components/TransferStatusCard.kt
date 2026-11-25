package com.example.hoppie.features.transfers.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppieLinkBlue
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.send.FileTransferState
import com.example.hoppie.features.send.components.StripedTransferProgressBar
import com.example.hoppie.features.send.formatFileSize
import com.example.hoppie.features.transfers.CompletedTransfer
import com.example.hoppie.features.transfers.fileCountLabel
import com.example.hoppie.features.transfers.summaryLine
import com.example.hoppie.features.transfers.summaryTitle

@Composable
internal fun OngoingTransferCard(transfer: FileTransferState, received: Boolean, onOpen: () -> Unit) {
    TransferCard(onOpen, leading = {
        Box(Modifier.size(52.dp).background(Color(0xFFEAF3FF), CircleShape), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.transfers_icon), null, Modifier.size(32.dp))
        }
    }) {
        val direction = if (received) "Receiving" else "Sending"
        Text("$direction ${fileCountLabel(transfer.files.size)}", style = MaterialTheme.typography.titleMedium)
        Text("${formatFileSize(transfer.bytesSent)} / ${formatFileSize(transfer.totalBytes)} · ${(transfer.progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium, color = HoppieFileSize)
        StripedTransferProgressBar(transfer.progress, Modifier.fillMaxWidth().padding(top = 3.dp).height(9.dp))
        Text("Return to active transfer", style = MaterialTheme.typography.labelMedium, color = HoppieLinkBlue,
            modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
internal fun TransferRecordCard(record: CompletedTransfer, date: String, onOpen: () -> Unit) {
    TransferCard(onOpen, leading = {
        val status = if (record.completed) "Successful transfer" else "Failed transfer"
        Box(Modifier.size(32.dp).background(if (record.completed) Color(0xFF397853) else Color(0xFFB5344C), CircleShape)
            .semantics { contentDescription = status }, contentAlignment = Alignment.Center) {
            Text(if (record.completed) "✓" else "!", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }) {
        Text(record.summaryTitle, style = MaterialTheme.typography.titleMedium)
        Text(record.summaryLine, style = MaterialTheme.typography.bodyMedium, color = HoppieFileSize)
        Text(date, style = MaterialTheme.typography.bodySmall, color = HoppieFileSize)
    }
}

@Composable
private fun TransferCard(onOpen: () -> Unit, leading: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        color = Color.White, contentColor = HoppiePurple, shadowElevation = 3.dp) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            leading()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp), content = content)
            Icon(painterResource(R.drawable.send_back_arrow), null, Modifier.size(18.dp).rotate(180f), tint = HoppieFileSize)
        }
    }
}
