package com.example.hoppie.features.transfers

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.components.BottomNavBar
import com.example.hoppie.core.components.FileCategory
import com.example.hoppie.core.navigation.AppNavGraph
import com.example.hoppie.core.navigation.MainDestination
import com.example.hoppie.core.theme.HOPPIETheme
import com.example.hoppie.features.send.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TransfersScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun compactHistoryFiltersScrollAndOpenFileDetails() {
        val file = CompletedTransferFile("A long filename that must stay readable on small phones.pdf", 2048)
        val entries = List(8) {
            CompletedTransfer("id-$it", it.toLong(), listOf(file), received = it % 2 == 0, peerName = "Device $it")
        } + CompletedTransfer("failed", 0, listOf(file.copy(verified = false)), completed = false)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            HOPPIETheme {
                Column(Modifier.size(320.dp, 480.dp)) {
                    Box(Modifier.weight(1f)) { TransfersContent(entries, null, null) {} }
                    BottomNavBar(MainDestination.Transfers) {}
                }
            }
        }
        compose.onNodeWithText("Completed").assertIsSelected()
        compose.onAllNodesWithText("Received 1 file").onFirst().assertIsDisplayed()
        val last = "2 KB · to Device 7"
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(last))
        compose.onNodeWithText("Transfer").assertIsSelected().assertIsDisplayed()
        compose.onNodeWithText(last).performClick()
        compose.onNodeWithText("Transfer details").assertIsDisplayed()
        compose.onNodeWithText(file.name).assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Failed"))
        compose.onNodeWithText("Failed").performClick()
        compose.onNodeWithText("Send failed · 1 file").assertIsDisplayed()
        compose.onNodeWithText("2 KB · to Unknown device").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Failed").assertIsSelected()
        compose.onNodeWithText("Ongoing").performClick()
        compose.onNodeWithText("No ongoing transfers.").assertIsDisplayed()
    }

    @Test fun activeCardUsesTransferEventsAndOpensTheCorrectRole() {
        val uri = Uri.parse("content://test/transfer")
        val file = SelectedFile(uri, "test.txt", "text/plain", 1024, FileCategory.Document)
        val transfer = mutableStateOf(FileTransferState(listOf(TransferFile(file))))
        var received: Boolean? = null
        compose.setContent { HOPPIETheme { TransfersContent(emptyList(), null, transfer.value) { received = it } } }
        compose.onNodeWithText("Ongoing").assertIsSelected()
        compose.onNodeWithText("0 B / 1 KB · 0%").assertIsDisplayed()
        compose.runOnIdle { transfer.value = transfer.value.applyEvent(FileTransferEvent.BytesSent(uri, 512)) }
        compose.onNodeWithText("512 B / 1 KB · 50%").assertIsDisplayed()
        compose.onNodeWithText("Receiving 1 file").performClick()
        compose.runOnIdle { assertEquals(true, received) }
    }

    @Test fun productionHistoryMatchesSavedRecordsAndUsesExistingNavigation() {
        val stored = CompletedTransferHistory(compose.activity).load()
        compose.setContent { HOPPIETheme { AppNavGraph {} } }
        compose.onNodeWithText("Transfer").performClick()
        compose.onNodeWithText("Transfer").assertIsSelected()
        compose.onNodeWithContentDescription("Notifications").assertIsDisplayed()
        for (filter in listOf(TransferStatusFilter.Completed, TransferStatusFilter.Failed)) {
            compose.onNodeWithText(filter.label).performClick()
            val record = stored.firstOrNull { it.completed == (filter == TransferStatusFilter.Completed) }
            if (record == null) {
                compose.waitUntil(5000) { compose.onAllNodesWithText(filter.emptyMessage).fetchSemanticsNodes().isNotEmpty() }
            } else {
                compose.onNode(hasScrollAction()).performScrollToNode(hasText(record.summaryLine))
                compose.onAllNodesWithText(record.summaryLine).onFirst().performClick()
                compose.onNodeWithText("Transfer details").assertIsDisplayed()
                compose.onNodeWithText("Close").performClick()
            }
        }
    }
}
