package com.example.hoppie.features.send.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.send.FileTransferState
import com.example.hoppie.features.send.TransferFile
import com.example.hoppie.features.send.TransferStatus
import com.example.hoppie.features.send.formatFileSize

@Composable
internal fun OverallTransferProgress(state: FileTransferState, modifier: Modifier = Modifier) {
    val count = state.files.size
    val label = if (count == 1) "file" else "files"
    val title = when (state.status) {
        TransferStatus.Preparing -> "Preparing $count $label"
        TransferStatus.Transferring -> "Transferring $count $label"
        TransferStatus.Verifying -> "Verifying $count $label"
        TransferStatus.Completed -> "Transfer Complete"
        TransferStatus.Failed -> "Failed to transfer files"
        TransferStatus.Cancelled -> "Transfer cancelled"
    }
    val percent = animateIntAsState((state.progress * 100).toInt(), tween(180), label = "transfer percentage")
    Column(modifier, Arrangement.spacedBy(10.dp), Alignment.CenterHorizontally) {
        Text(
            title,
            style = if (state.status == TransferStatus.Completed || state.status == TransferStatus.Failed)
                MaterialTheme.typography.headlineSmall
                else MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            "${percent.value}%",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 40.sp, lineHeight = 46.sp)
        )
        Text(
            "${formatFileSize(state.bytesSent)} of ${formatFileSize(state.totalBytes)}",
            style = MaterialTheme.typography.bodyLarge,
            color = HoppieFileSize,
            textAlign = TextAlign.Center
        )
        StripedTransferProgressBar(state.progress, Modifier.fillMaxWidth().height(18.dp))
        state.message?.let {
            Text(it, color = HoppieFileSize, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun TransferFileItem(
    transfer: TransferFile,
    modifier: Modifier = Modifier,
    showVerificationStatus: Boolean = false
) {
    val file = transfer.file
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        contentColor = HoppiePurple,
        shadowElevation = 4.dp
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(file.category.icon), null, Modifier.size(32.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(file.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatFileSize(transfer.bytesSent)} / ${formatFileSize(file.sizeBytes)} (${(transfer.progress * 100).toInt()}%)",
                    color = HoppieFileSize,
                    style = MaterialTheme.typography.bodyMedium
                )
                StripedTransferProgressBar(
                    transfer.progress,
                    Modifier.fillMaxWidth(0.78f).height(8.dp),
                    border = 1.dp
                )
            }
            if (showVerificationStatus && transfer.verified) VerifiedFileIcon()
        }
    }
}
