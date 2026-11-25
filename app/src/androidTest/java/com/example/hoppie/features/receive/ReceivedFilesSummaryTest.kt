package com.example.hoppie.features.receive

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.components.OfferedFile
import com.example.hoppie.core.theme.HOPPIETheme
import com.example.hoppie.features.send.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReceivedFilesSummaryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun file(id: String, name: String, size: Long) =
        TransferFile(OfferedFile(id, name, size, null).asTransferFile())

    private fun show(state: FileTransferState) {
        compose.setContent {
            HOPPIETheme {
                Box(Modifier.size(320.dp, 480.dp)) { ReceivedFilesSummary(state, "OPPO", {}) }
            }
        }
    }

    private fun scroll(text: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(text))
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    @Test fun singlePdfHasVerifiedSizeSenderAndRealOpenAction() {
        val pdf = file("1", "Ticket.pdf", 1024)
        val saved = Uri.parse("content://media/external/downloads/1")
        show(FileTransferState(listOf(pdf)).applyEvent(FileTransferEvent.Verified(pdf.file.uri, saved))
            .copy(status = TransferStatus.Completed))
        compose.onNodeWithText("Transfer Complete!").assertIsDisplayed()
        scroll("Your files have been received successfully.")
        scroll("Sender")
        scroll("OPPO")
        scroll("Files received")
        scroll("1")
        scroll("Total received")
        scroll("1 KB")
        scroll("Ticket.pdf")
        compose.onNodeWithText("Open").assertHasClickAction()
        compose.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test fun mixedBatchScrollsToLongLastFilenameAndDisplaysLargeSize() {
        val name = "Field notes for the November survey including the final corrected appendix.txt"
        val files = (1..8).map { file(it.toString(), "Image-$it.png", 1024) } + file("9", name, 50L * 1024 * 1024)
        show(FileTransferState(files.map { it.copy(verified = true) }, TransferStatus.Completed))
        scroll(name)
        scroll("TXT · 50 MB")
        compose.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test fun interruptedBatchKeepsOnlyVerifiedBytesAndFilesInReceivedCount() {
        val first = file("a", "Survey.pdf", 100)
        val second = file("b", "Photo.jpg", 300)
        var state = FileTransferState(listOf(first, second))
            .applyEvent(FileTransferEvent.Verified(first.file.uri))
            .applyEvent(FileTransferEvent.BytesSent(second.file.uri, 150))
        assertEquals(0.5f, state.files.last().progress, 0.001f)
        assertEquals(0.625f, state.progress, 0.001f)
        state = state.copy(status = TransferStatus.Failed)
        assertEquals(1, state.verifiedCount)
        assertEquals(100L, state.verifiedBytes)
        show(state)
        compose.onNodeWithText("Transfer Complete!").assertDoesNotExist()
        compose.onNodeWithText("Transfer interrupted").assertIsDisplayed()
        val interruptedBounds = compose.onNodeWithTag("completion_illustration_failed_mail")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(interruptedBounds.height > 0f)
        scroll("1")
        scroll("100 B")
        scroll("Saved and verified")
        scroll("Not completed")
        scroll("Photo.jpg")
        scroll("Unverified")
    }

    @Test fun emptyFailedAttemptShowsNoSuccessOrFileAction() {
        show(FileTransferState(emptyList(), TransferStatus.Failed))
        compose.onNodeWithText("Transfer Complete!").assertDoesNotExist()
        scroll("No files were saved.")
        compose.onNodeWithText("Open").assertDoesNotExist()
    }
}
