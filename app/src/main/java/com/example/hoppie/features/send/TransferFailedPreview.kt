package com.example.hoppie.features.send

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.hoppie.core.components.FileCategory
import com.example.hoppie.core.theme.HOPPIETheme

@Preview(name = "Transfer failure flow", showBackground = true, widthDp = 393, heightDp = 820)
@Preview(name = "Transfer failure compact", showBackground = true, widthDp = 320, heightDp = 600)
@Composable
internal fun TransferFailedPreview() {
    val fileSize = 10L * 1024 * 1024
    val files = remember {
        listOf("Notes.pdf", "Draft.docx", "Photo.png", "Slides.pptx").mapIndexed { index, name ->
            val file = SelectedFile(Uri.parse("preview://transfer/$index"), name, null, fileSize, FileCategory.from(name))
            TransferFile(
                file,
                bytesSent = listOf(fileSize, fileSize / 2, fileSize / 10, 0L)[index],
                verified = index == 0
            )
        }
    }
    var status by remember { mutableStateOf(TransferStatus.Failed) }
    val state = FileTransferState(files, status = status)
    HOPPIETheme {
        if (status == TransferStatus.Failed) {
            TransferFailedScreen(
                state,
                onRetry = { status = TransferStatus.Transferring },
                onExit = { status = TransferStatus.Cancelled }
            )
        } else {
            TransferringFilesScreen(
                state,
                onCancel = { status = TransferStatus.Failed },
                onDone = { status = TransferStatus.Failed },
                exitLabel = "Back to failed preview"
            )
        }
    }
}
