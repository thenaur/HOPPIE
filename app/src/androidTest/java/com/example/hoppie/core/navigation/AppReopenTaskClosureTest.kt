package com.example.hoppie.core.navigation

import android.net.Uri
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.hoppie.HoppieApplication
import com.example.hoppie.MainActivity
import com.example.hoppie.core.components.FileCategory
import com.example.hoppie.core.components.FileOfferDecision
import com.example.hoppie.core.components.OfferedFile
import com.example.hoppie.data.nearby.Phase
import com.example.hoppie.features.send.FileTransferEvent
import com.example.hoppie.features.send.FileTransferState
import com.example.hoppie.features.send.OutgoingFilesActions
import com.example.hoppie.features.send.SelectedFile
import com.example.hoppie.features.send.SendViewModel
import com.example.hoppie.features.send.TransferFile
import com.example.hoppie.features.send.TransferStatus
import com.example.hoppie.features.transfers.CompletedTransferHistory
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppReopenTaskClosureTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null
    private var tempFile: File? = null

    @After
    fun cleanup() {
        scenario?.close()
        tempFile?.delete()
    }

    @Test
    fun closingWindowDuringActiveTransferStartsNextLaunchAtHomeAndPreservesHistory() {
        val first = ActivityScenario.launch(MainActivity::class.java)
        scenario = first

        lateinit var app: HoppieApplication
        lateinit var sender: SendViewModel
        var historyBefore = 0
        val seededPeer = "ClosurePeer-${System.currentTimeMillis()}"
        first.onActivity { activity ->
            activity.setTurnScreenOn(true)
            activity.setShowWhenLocked(true)
            app = activity.application as HoppieApplication
            val history = CompletedTransferHistory(app)
            historyBefore = history.load().size
            val sample = SelectedFile(
                Uri.parse("content://test/Preserved.pdf"), "Preserved.pdf", "application/pdf", 2048L, FileCategory.Pdf
            )
            history.record(
                FileTransferState(listOf(TransferFile(sample, bytesSent = 2048L, verified = true)), TransferStatus.Completed, recipientName = seededPeer),
                received = true,
                peerName = seededPeer
            )
            val payload = File.createTempFile("hoppie-close-", ".txt", activity.cacheDir).apply {
                writeBytes(ByteArray(1024) { 'C'.code.toByte() })
            }
            tempFile = payload
            sender = ViewModelProvider(activity)[SendViewModel::class.java]
            sender.addFiles(listOf(Uri.fromFile(payload)))
        }

        compose.waitUntil(5_000) { sender.state.value.files.size == 1 }
        compose.onNodeWithText("Send Files").performClick()
        compose.waitForIdle()

        val actions = object : OutgoingFilesActions {
            val decision = CompletableDeferred<FileOfferDecision>()
            var offered = emptyList<OfferedFile>()
            val events = Channel<FileTransferEvent>(Channel.UNLIMITED)
            override suspend fun offerFiles(endpointId: String, manifest: List<OfferedFile>): FileOfferDecision {
                offered = manifest
                return decision.await()
            }
            override fun transfer(endpointId: String, files: Map<String, SelectedFile>): Flow<FileTransferEvent> =
                flow { emitAll(events.receiveAsFlow()) }
            override suspend fun cancelTransfer(endpointId: String): Boolean = true
        }
        compose.runOnIdle {
            sender.offerConnectedFiles(100L, "peer-close", "Peer") { actions }
        }
        compose.waitUntil(5_000) { actions.offered.isNotEmpty() }
        compose.runOnIdle {
            actions.decision.complete(FileOfferDecision.Accepted(setOf(actions.offered.single().id)))
        }
        compose.waitUntil(5_000) { sender.state.value.transfer?.active == true }

        // Remove the app task (like swiping HOPPIE away from Recent Apps) while transfer is active.
        first.onActivity { it.finishAndRemoveTask() }
        compose.waitUntil(5_000) { first.state == Lifecycle.State.DESTROYED }
        first.close()
        scenario = null

        assertEquals(Phase.Idle, app.nearby.state.value.phase)

        // Next launch must start at Home even with prior completed transfer history on disk.
        val relaunched = ActivityScenario.launch(MainActivity::class.java)
        scenario = relaunched
        relaunched.onActivity {
            it.setTurnScreenOn(true)
            it.setShowWhenLocked(true)
        }

        compose.waitUntilAtLeastOneExists(hasText("Send Files"), 5_000)
        compose.onNodeWithText("Send Files").assertIsDisplayed()
        compose.onNodeWithText("Receive Files").assertIsDisplayed()

        relaunched.onActivity { activity ->
            assertNull(ViewModelProvider(activity)[AppReopenCoordinator::class.java].activeReturnTarget)
            assertNull(ViewModelProvider(activity)[SendViewModel::class.java].state.value.transfer)
            assertEquals(historyBefore + 1, CompletedTransferHistory(activity).load().size)
        }

        // History remains intact and Ongoing is empty.
        compose.onNodeWithText("Transfer").performClick()
        compose.onNodeWithText("Ongoing").performClick()
        compose.onNodeWithText("No ongoing transfers.").assertIsDisplayed()
        compose.onNodeWithText("Completed").performClick()
        compose.onNodeWithText("2 KB · from $seededPeer").assertIsDisplayed()
    }
}
