package com.example.hoppie.core.components

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppieLinkBlue
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.send.FileTransferState
import com.example.hoppie.features.send.TransferActionButton
import com.example.hoppie.features.send.TransferFile
import com.example.hoppie.features.send.TransferStatus
import com.example.hoppie.features.send.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun TransferCompletionScreen(
    state: FileTransferState,
    peerName: String?,
    onDone: () -> Unit,
    onOpenReceived: ((Uri, String?) -> Unit)? = null
) {
    val receiving = onOpenReceived != null
    val direction = if (receiving) "received" else "sent"
    BackHandler { if (state.status != TransferStatus.Completed) onDone() }
    val complete = state.status == TransferStatus.Completed && state.files.isNotEmpty() &&
        state.verifiedCount == state.files.size
    val (verified, unfinished) = state.files.partition { it.verified }
    FileTransferBackground {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CompletionHeader(complete, direction, state.message)
                }
                item {
                    TransferSummaryCard(
                        fileCount = state.verifiedCount,
                        formattedTotalSize = formatFileSize(state.verifiedBytes),
                        receiving = receiving,
                        peerName = peerName
                    )
                }
                item { FileSectionHeading(if (receiving) "Received files" else "Sent files") }
                if (verified.isEmpty()) {
                    item { Text(if (receiving) "No files were saved." else "No files were verified.", color = HoppieFileSize) }
                }
                items(verified, key = { it.file.uri.toString() }) { CompletionFileRow(it, onOpenReceived) }
                if (unfinished.isNotEmpty()) {
                    item { FileSectionHeading("Not completed") }
                    items(unfinished, key = { it.file.uri.toString() }) { CompletionFileRow(it, onOpenReceived) }
                }
            }
            TransferActionButton("Done", onClick = onDone, primary = true)
        }
    }
}

@Composable
private fun FileSectionHeading(title: String) {
    Text(title, Modifier.padding(top = 8.dp).semantics { heading() },
        style = MaterialTheme.typography.titleMedium, color = HoppiePurple)
}

@Composable
private fun CompletionHeader(complete: Boolean, direction: String, message: String?) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painterResource(if (complete) R.drawable.transfer_completed else R.drawable.failed_mail),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .heightIn(max = 196.dp)
                .testTag(if (complete) "completion_illustration_complete" else "completion_illustration_failed_mail"),
            contentScale = ContentScale.Fit
        )
        Text(
            if (complete) "Transfer Complete!" else "Transfer interrupted",
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall, color = HoppiePurple, textAlign = TextAlign.Center
        )
        Text(
            if (complete) "Your files have been $direction successfully." else message ?: "Only verified files are available.",
            style = MaterialTheme.typography.bodyMedium, color = HoppieFileSize, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CompletionFileRow(item: TransferFile, onOpenReceived: ((Uri, String?) -> Unit)?) {
    val file = item.file
    val saved = item.savedUri?.takeIf { item.verified && it.scheme == "content" }
    val action = if (saved != null && onOpenReceived != null) Modifier.clickable(onClickLabel = "Open file") {
        onOpenReceived(saved, file.mimeType)
    } else Modifier
    val status = when {
        !item.verified -> "Unverified"
        onOpenReceived != null -> "Saved and verified"
        else -> "Sent and verified"
    }
    Row(
        Modifier.fillMaxWidth().then(action).heightIn(min = 64.dp).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FileThumbnail(file.category, if (onOpenReceived == null) file.uri else saved)
        Column(Modifier.weight(1f)) {
            Text(item.originalName, color = HoppiePurple, style = MaterialTheme.typography.titleSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.originalName != file.name) Text("Saved as ${file.name}", style = MaterialTheme.typography.bodySmall,
                color = HoppieFileSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val type = item.originalName.substringAfterLast('.', "").uppercase().ifBlank { file.mimeType ?: "File" }
            Text("$type · ${formatFileSize(file.sizeBytes)}", color = HoppieFileSize, style = MaterialTheme.typography.bodySmall)
            Text(status, color = if (item.verified) Color(0xFF247C73) else HoppieFileSize,
                style = MaterialTheme.typography.labelMedium)
        }
        if (saved != null && onOpenReceived != null) Text("Open", color = HoppieLinkBlue, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun FileThumbnail(category: FileCategory, uri: Uri?) {
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(null, uri, category) {
        value = if (uri != null && category == FileCategory.Image && Build.VERSION.SDK_INT >= 29) {
            withContext(Dispatchers.IO) {
                try { context.contentResolver.loadThumbnail(uri, Size(120, 120), null) }
                catch (_: java.io.IOException) { null }
                catch (_: RuntimeException) { null }
            }
        } else null
    }
    val bitmap = thumbnail
    if (bitmap == null) Image(painterResource(category.icon), null, Modifier.size(42.dp))
    else Image(bitmap.asImageBitmap(), null, Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
}
