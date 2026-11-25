package com.example.hoppie.features.receive

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.hoppie.core.components.TransferCompletionScreen
import com.example.hoppie.core.navigation.LocalSuppressReopen
import com.example.hoppie.features.send.FileTransferState

@Composable
internal fun ReceivedFilesSummary(state: FileTransferState, sender: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val suppressReopen = LocalSuppressReopen.current
    TransferCompletionScreen(state, peerName = sender, onDone = onDone, onOpenReceived = { uri, mime ->
        openReceipt(context, uri, mime, suppressReopen)
    })
}

private fun openReceipt(context: Context, uri: Uri, mime: String?, suppressReopen: (Boolean) -> Unit) {
    try {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { } ?: throw java.io.FileNotFoundException()
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, context.contentResolver.getType(uri) ?: mime ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        suppressReopen(true)
        context.startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        suppressReopen(false)
        Toast.makeText(context, "No viewer is available for this file.", Toast.LENGTH_LONG).show()
    } catch (_: Exception) {
        suppressReopen(false)
        Toast.makeText(context, "This file is no longer accessible.", Toast.LENGTH_LONG).show()
    }
}
