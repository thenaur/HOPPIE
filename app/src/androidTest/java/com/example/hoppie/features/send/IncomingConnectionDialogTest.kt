package com.example.hoppie.features.send

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.hoppie.core.components.FileCategory
import com.example.hoppie.core.components.FileOfferDecision
import com.example.hoppie.core.components.HoppieNotice
import com.example.hoppie.core.components.OfferedFile
import com.example.hoppie.core.theme.HOPPIETheme
import com.example.hoppie.data.nearby.NearbyRole
import com.example.hoppie.data.nearby.Peer
import com.example.hoppie.data.nearby.Phase
import com.example.hoppie.data.nearby.SessionState
import com.example.hoppie.features.send.components.NearbyConnectionDialog
import com.example.hoppie.features.send.components.StripedTransferProgressBar
import com.example.hoppie.features.transfers.CompletedTransferHistory
import com.example.hoppie.features.home.HomeScreen
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomingConnectionDialogTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun acceptWaitsForConfirmationAndIgnoresAnotherRequest() {
        val request = IncomingConnectionRequest("first", "Nearby test phone with a deliberately long device name that wraps across multiple lines")
        val actions = TestConnectionActions(waitForReceiver = true)
        val model = showSendScreen()
        compose.onNodeWithText("Accept").assertDoesNotExist()

        compose.runOnIdle {
            model.onIncomingConnectionRequest(request, actions)
            assertFalse(
                model.onIncomingConnectionRequest(IncomingConnectionRequest("second", "Other phone"), actions)
            )
        }

        compose.onNodeWithText(request.deviceName).assertIsDisplayed()
        compose.onNodeWithText("Accept").performClick()
        compose.onNodeWithText("Connecting…").assertIsNotEnabled()
        compose.onNodeWithText("Decline Request").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals("first", actions.acceptedEndpoint)
            assertNull(actions.connectedEndpoint)
            actions.acceptResult.complete(ConnectionResponse.Completed)
        }

        compose.onNodeWithText("Connecting…").assertDoesNotExist()
        compose.onNodeWithText("Waiting for receiver").assertIsDisplayed()
        compose.waitUntil(timeoutMillis = 5_000) { actions.manifest.isNotEmpty() }
        compose.onNodeWithText("Transferring Files").assertDoesNotExist()
        compose.runOnIdle {
            assertNull(actions.connectedEndpoint)
            val file = model.state.value.files.single()
            assertEquals(file.name, actions.manifest.single().name)
            assertEquals(file.sizeBytes, actions.manifest.single().sizeBytes)
            actions.receiverDecision.complete(FileOfferDecision.Accepted(setOf(actions.manifest.single().id)))
        }
        compose.onNodeWithText("Transferring Files").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("first", actions.connectedEndpoint)
            assertEquals(model.state.value.files, actions.sharedFiles)
            assertEquals(1, model.state.value.files.size)
            val file = actions.sharedFiles.single()
            actions.events.trySend(FileTransferEvent.BytesSent(file.uri, file.sizeBytes))
            actions.events.trySend(FileTransferEvent.Verifying(file.uri))
        }
        val history = CompletedTransferHistory(compose.activity.application)
        val recordsBefore = history.load().size
        var recordsAtCleanup: Int? = null
        actions.onFinishSession = { recordsAtCleanup = history.load().size }
        compose.runOnIdle {
            val state = requireNotNull(model.state.value.transfer)
            assertEquals(TransferStatus.Verifying, state.status)
            assertEquals(99, (state.progress * 100).toInt())
            actions.events.trySend(FileTransferEvent.Verified(state.files.single().file.uri))
        }
        compose.onNodeWithText("Transfer Complete!").assertDoesNotExist()
        compose.runOnIdle {
            assertNull(recordsAtCleanup)
            actions.events.close()
        }

        compose.onNodeWithText("Transfer Complete!").assertIsDisplayed()
        compose.onNodeWithText("Recipient").assertIsDisplayed()
        compose.onNodeWithText(request.deviceName).assertIsDisplayed()
        compose.onNodeWithText("Sent files").assertIsDisplayed()
        compose.onNodeWithText("Sent and verified").assertIsDisplayed()
        compose.runOnIdle {
            val state = requireNotNull(model.state.value.transfer)
            assertEquals(TransferStatus.Completed, state.status)
            val file = state.files.single()
            val records = history.load()
            assertEquals(recordsBefore + 1, recordsAtCleanup)
            assertEquals(recordsBefore + 1, records.size)
            assertEquals(file.file.name, records.first().files.single().name)
            assertEquals(file.bytesSent, records.first().totalBytes)
        }
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Receive Files").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(SendUiState(), model.state.value)
            assertEquals(recordsBefore + 1, history.load().size)
        }
    }

    @Test
    fun sendingListScrollsUnderFixedStopAndVerificationUsesSessionDigits() {
        val files = List(12) { index ->
            SelectedFile(Uri.parse("content://test/$index"), "Long selected filename $index.pdf", null, 2048, FileCategory.Pdf)
        }
        val searching = SessionState(role = NearbyRole.Advertiser, phase = Phase.Searching)
        val prompt = searching.copy(phase = Phase.Verify, peer = Peer("endpoint", "Test phone"), digits = "0716")
        val session = mutableStateOf(searching)
        var stopped = false
        var decision: Pair<SessionState, Boolean>? = null
        compose.setContent {
            HOPPIETheme {
                Box(Modifier.width(280.dp).height(450.dp)) {
                    SendingFilesScreen(SendUiState(files = files), session.value) { stopped = true }
                    if (session.value.phase == Phase.Verify) {
                        NearbyConnectionDialog(session.value) { request, accepted ->
                            decision = request to accepted
                            session.value = searching
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Sending Files").assertIsDisplayed()
        compose.onNodeWithText("Ask the receiver to tap Connect in HOPPIE.").assertIsDisplayed()
        compose.onNodeWithText("Ask the receiver to tap Receive in HOPPIE.").assertDoesNotExist()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Sharing files (12)"))
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(files.last().name))
        compose.onNodeWithText(files.last().name).assertIsDisplayed()
        compose.onNodeWithText("Stop Sharing").assertIsDisplayed()
        for (accept in listOf(false, true)) {
            compose.runOnIdle { session.value = prompt }
            compose.onNodeWithText("Test phone").assertIsDisplayed()
            compose.onNodeWithText("Compare this code on both phones").assertIsDisplayed()
            compose.onNodeWithText(prompt.digits).assertIsDisplayed()
            compose.onNodeWithText(if (accept) "Accept" else "Decline Request").performClick()
            compose.runOnIdle { assertEquals(prompt to accept, decision) }
        }
        compose.onNodeWithText("Stop Sharing").performClick()
        compose.runOnIdle { assertTrue(stopped) }
    }

    @Test
    fun completionListScrollsUnderFixedDoneButtonInCompactLayout() {
        val files = List(12) { index ->
            val name = "Completed file $index with a long filename for narrow screens.pdf"
            val file = SelectedFile(Uri.parse("content://test/$index"), name, null, 128, FileCategory.Pdf)
            TransferFile(file, bytesSent = 128, verified = true)
        }
        val state = FileTransferState(files, status = TransferStatus.Completed, recipientName = "Test recipient")
        var done = false
        compose.setContent {
            HOPPIETheme {
                Box(Modifier.width(280.dp).height(450.dp)) {
                    TransferringFilesScreen(state, onCancel = {}, onDone = { done = true })
                }
            }
        }

        compose.onNodeWithText("Done").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Your files have been sent successfully."))
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Recipient"))
        for (text in listOf("12", "Files sent", "1.5 KB", "Total sent", "Test recipient")) {
            compose.onNodeWithText(text).assertIsDisplayed()
        }
        val lastName = files.last().file.name
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(lastName))
        compose.onNodeWithText(lastName).assertIsDisplayed()
        compose.onNodeWithText("Done").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(done) }
    }

    @Test
    fun stripesAreClippedToEachProgressFill() {
        compose.setContent {
            Column {
                for (value in listOf(0f, 0.4f, 1f)) {
                    StripedTransferProgressBar(
                        value,
                        Modifier.width(220.dp).height(24.dp).testTag(value.toString())
                    )
                }
            }
        }

        val dark = Color(0xFF4F75E8)
        val light = Color(0xFF83A9FF)
        val empty = compose.onNodeWithTag("0.0").captureToImage().toPixelMap()
        val partial = compose.onNodeWithTag("0.4").captureToImage().toPixelMap()
        val full = compose.onNodeWithTag("1.0").captureToImage().toPixelMap()
        val middle = partial.height / 2
        assertEquals(Color.White, empty[empty.width / 4, middle])
        assertEquals(Color.White, partial[partial.width * 3 / 4, middle])
        assertTrue(full[full.width * 3 / 4, middle] in setOf(dark, light))
        val filledColumns = partial.width / 5 until partial.width / 3
        assertTrue(filledColumns.any { partial[it, middle] == dark })
        assertTrue(filledColumns.any { partial[it, middle] == light })
        assertTrue(filledColumns.any { partial[it, middle - 4] != partial[it, middle + 4] })
        assertTrue(filledColumns.none { partial[it, 0] in setOf(dark, light) })
    }

    @Test
    fun authenticatedSessionOffersBatchOnceAndWaitsForApprovedFiles() {
        val model = showSendScreen()
        val extra = File.createTempFile("batch-", ".txt", compose.activity.cacheDir)
        val actions = TestConnectionActions(waitForReceiver = true)
        var offers = 0
        try {
            extra.writeText("Second selected file")
            compose.runOnIdle { model.addFiles(listOf(Uri.fromFile(extra))) }
            compose.waitUntil(5_000) { model.state.value.files.size == 2 }
            fun offer() = model.offerConnectedFiles(1, "peer", "Test recipient") {
                offers++
                actions
            }
            compose.runOnIdle {
                offer()
                offer()
            }
            compose.waitUntil(5_000) { actions.manifest.size == 2 }
            compose.runOnIdle {
                assertNull(model.state.value.transfer)
                assertNull(actions.connectedEndpoint)
                actions.receiverDecision.complete(FileOfferDecision.Accepted(actions.manifest.map { it.id }.toSet()))
            }
            compose.waitUntil(5_000) { actions.sharedFiles.size == 2 }
            compose.runOnIdle {
                assertEquals(model.state.value.files, actions.sharedFiles)
                for (file in actions.sharedFiles) actions.events.trySend(FileTransferEvent.Verified(file.uri))
                actions.events.close()
            }
            compose.waitUntil(5_000) { model.state.value.transfer?.status == TransferStatus.Completed }
            compose.runOnIdle {
                model.finishCompletedTransfer()
                offer()
                assertEquals(1, offers)
                assertNull(model.state.value.transfer)
            }
        } finally {
            extra.delete()
        }
    }

    @Test
    fun aggregateProgressWeightsIndependentFilesBySize() {
        val model = showSendScreen()
        val first = model.state.value.files.single().copy(sizeBytes = 100)
        val second = first.copy(uri = Uri.parse("content://test/second"), sizeBytes = 300)
        val initial = FileTransferState(listOf(TransferFile(first), TransferFile(second)))
        val state = initial
            .applyEvent(FileTransferEvent.BytesSent(first.uri, 50))
            .applyEvent(FileTransferEvent.BytesSent(second.uri, 30))
        assertEquals(0.5f, state.files[0].progress, 0.001f)
        assertEquals(0.1f, state.files[1].progress, 0.001f)
        assertEquals(0.2f, state.progress, 0.001f)
        assertEquals(400L, state.totalBytes)
    }

    @Test
    fun failedPreviewRetainsProgressAndKeepsActionsAccessible() {
        compose.setContent { TransferFailedPreview() }
        compose.onNodeWithText("Failed to transfer files").assertIsDisplayed()
        compose.onNodeWithText("40%").assertIsDisplayed()
        compose.onNodeWithText("16 MB of 40 MB").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Slides.pptx"))
        compose.onNodeWithText("Slides.pptx").assertIsDisplayed()
        compose.onNodeWithText("Retry Transfer").assertIsDisplayed().performClick()
        compose.onNodeWithText("Failed to transfer files").assertDoesNotExist()
        compose.onNodeWithText("Transferring 4 files").assertIsDisplayed()
        compose.onNodeWithText("Cancel Transfer").performClick()
        compose.onNodeWithText("Cancel Transfer").assertIsDisplayed().performClick()
        compose.onNodeWithText("Leave this transfer?").assertIsDisplayed()
        compose.onNodeWithText("Stay").performClick()
        compose.onNodeWithText("Failed to transfer files").assertIsDisplayed()
        compose.onNodeWithText("Cancel Transfer").performClick()
        compose.onNodeWithText("Leave transfer").performClick()
        compose.onNodeWithText("Transfer cancelled").assertIsDisplayed()
    }

    @Test
    fun cancellationWaitsForTransport() {
        val model = showSendScreen()
        val actions = TestConnectionActions()
        compose.runOnIdle {
            model.onIncomingConnectionRequest(IncomingConnectionRequest("first", "Test phone"), actions)
            actions.acceptResult.complete(ConnectionResponse.Completed)
        }
        compose.onNodeWithText("Accept").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { model.state.value.transfer != null }
        compose.onNodeWithText("Cancel Transfer").performClick()
        compose.onNodeWithText("Cancelling…").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(TransferStatus.Preparing, model.state.value.transfer?.status)
            actions.cancelResult.complete(true)
        }
        compose.runOnIdle {
            assertEquals(TransferStatus.Cancelled, model.state.value.transfer?.status)
            assertEquals(1, model.state.value.files.size)
        }
        compose.onNodeWithText("Transfer cancelled").assertIsDisplayed()
        compose.onNodeWithText("Back to Send Files").assertIsDisplayed()
        compose.onNodeWithTag("transfer_illustration_failed_mail").assertIsDisplayed()
    }

    @Test
    fun aPendingFailureCanBeDeclinedAndLaterRequestsCanEnd() {
        val request = IncomingConnectionRequest("first", "Nearby test tablet")
        val actions = TestConnectionActions()
        val model = showSendScreen()
        compose.runOnIdle {
            actions.acceptResult.complete(ConnectionResponse.Failed(requestStillPending = true))
            model.onIncomingConnectionRequest(request, actions)
        }

        compose.onNodeWithText("Accept").performClick()
        compose.onNodeWithText("Could not connect. Try again.").assertIsDisplayed()
        compose.onNodeWithText("Decline Request").performClick()
        compose.onNodeWithText("Declining…").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals("first", actions.declinedEndpoint)
            actions.declineResult.complete(ConnectionResponse.Completed)
        }
        compose.onNodeWithText("Declining…").assertDoesNotExist()

        for (timedOut in listOf(false, true)) {
            val next = IncomingConnectionRequest("next", "Another nearby device")
            compose.runOnIdle { model.onIncomingConnectionRequest(next, actions) }
            compose.onNodeWithText(next.deviceName).assertIsDisplayed()
            compose.runOnIdle { model.onIncomingConnectionEnded(next, timedOut) }
            compose.onNodeWithText(next.deviceName).assertDoesNotExist()
        }
        compose.runOnIdle {
            assertNull(actions.connectedEndpoint)
            assertEquals(1, model.state.value.files.size)
        }
    }

    @Test
    fun receiverDeclineEndsOfferSessionAndPreservesSelectedFiles() {
        val model = showSendScreen()
        val actions = TestConnectionActions(waitForReceiver = true)
        var finished = 0
        actions.onFinishSession = { finished++ }
        compose.runOnIdle {
            model.offerConnectedFiles(99L, "peer-1", "Anna's Phone") { actions }
        }
        compose.waitUntil(5_000) { actions.manifest.isNotEmpty() }
        compose.runOnIdle {
            actions.receiverDecision.complete(FileOfferDecision.Declined)
        }
        compose.waitUntil(5_000) { !model.state.value.waitingForReceiver }
        compose.onNodeWithTag("hoppie_notice_files_declined").assertIsDisplayed()
        compose.onNodeWithText("Anna's Phone").assertIsDisplayed()
        compose.onNodeWithText("has declined to receive the files").assertIsDisplayed()
        compose.onNodeWithContentDescription("Dismiss notification").performClick()
        compose.onNodeWithTag("hoppie_notice_files_declined").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, finished)
            assertEquals(1, model.state.value.files.size)
            assertNull(model.state.value.transfer)
        }
    }

    @Test
    fun sendScreenRendersConnectionDeclinedAndLostCardsAndAllowsRetry() {
        val app = compose.activity.application
        val model = SendViewModel(app)
        val file = File(app.cacheDir, "notice-card-test.txt").apply { writeText("Selected file") }
        compose.runOnIdle { model.addFiles(listOf(Uri.fromFile(file))) }
        compose.waitUntil(timeoutMillis = 5_000) { model.state.value.files.size == 1 }
        val idleNotice = mutableStateOf<HoppieNotice?>(HoppieNotice.ConnectionDeclined("Haribon's Phone"))
        var advertised = 0
        compose.setContent {
            HOPPIETheme {
                SendScreen(
                    onBack = {},
                    model = model,
                    onAdvertise = { advertised++ },
                    idleNotice = idleNotice.value,
                    onDismissNotice = { idleNotice.value = null }
                )
            }
        }
        compose.onNodeWithTag("hoppie_notice_connection_declined").assertIsDisplayed()
        compose.onNodeWithText("Haribon's Phone").assertIsDisplayed()
        compose.onNodeWithText("has declined request to connect").assertIsDisplayed()
        compose.onNodeWithText("Send").assertIsEnabled()

        compose.runOnIdle { idleNotice.value = HoppieNotice.ConnectionLost() }
        compose.onNodeWithTag("hoppie_notice_connection_lost").assertIsDisplayed()
        compose.onNodeWithText("Connection Lost").assertIsDisplayed()
        compose.onNodeWithText("Your transfer was interrupted.\nOnly verified files are available.").assertIsDisplayed()
        compose.onNodeWithText("Your progress is saved", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Send").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertNull(idleNotice.value)
            assertEquals(1, advertised)
        }
        compose.onNodeWithTag("hoppie_notice_connection_lost").assertDoesNotExist()
    }

    @Test
    fun repeatedDoneClicksFromSenderCompletionTriggerNavigationOnce() {
        val app = compose.activity.application
        val model = SendViewModel(app)
        val file = File(app.cacheDir, "sender-done-idempotent.txt").apply { writeText("Payload") }
        val actions = TestConnectionActions(waitForReceiver = true)
        var doneCount = 0
        try {
            compose.runOnIdle { model.addFiles(listOf(Uri.fromFile(file))) }
            compose.waitUntil(5_000) { model.state.value.files.size == 1 }
            compose.setContent {
                HOPPIETheme {
                    SendScreen(onBack = {}, model = model, onTransferDone = { doneCount++ })
                }
            }
            compose.runOnIdle { model.offerConnectedFiles(101L, "peer", "Recipient") { actions } }
            compose.waitUntil(5_000) { actions.manifest.size == 1 }
            compose.runOnIdle {
                actions.receiverDecision.complete(FileOfferDecision.Accepted(setOf(actions.manifest.single().id)))
            }
            compose.waitUntil(5_000) { actions.sharedFiles.size == 1 }
            compose.runOnIdle {
                actions.events.trySend(FileTransferEvent.Verified(actions.sharedFiles.single().uri))
                actions.events.close()
            }
            compose.waitUntil(5_000) { model.state.value.transfer?.status == TransferStatus.Completed }

            compose.onNodeWithText("Done").assertIsDisplayed().performClick()
            compose.onNodeWithText("Done").performClick()
            compose.runOnIdle {
                assertEquals(1, doneCount)
                assertNull(model.state.value.transfer)
            }
        } finally {
            file.delete()
        }
    }

    private fun showSendScreen(): SendViewModel {
        val app = compose.activity.application
        val model = SendViewModel(app)
        val file = File(app.cacheDir, "incoming-dialog-test.txt").apply { writeText("Selected file") }
        compose.runOnIdle { model.addFiles(listOf(Uri.fromFile(file))) }
        compose.waitUntil(timeoutMillis = 5_000) { model.state.value.files.size == 1 }
        compose.setContent {
            val done = remember { mutableStateOf(false) }
            HOPPIETheme {
                if (done.value) HomeScreen(onSend = {}, onReceive = {}, onSeeAll = {})
                else SendScreen(onBack = {}, model = model, onTransferDone = { done.value = true })
            }
        }
        return model
    }

    private class TestConnectionActions(private val waitForReceiver: Boolean = false) : IncomingConnectionActions {
        val receiverDecision = CompletableDeferred<FileOfferDecision>()
        var manifest: List<OfferedFile> = emptyList()

        override suspend fun offerFiles(endpointId: String, manifest: List<OfferedFile>): FileOfferDecision {
            this.manifest = manifest
            return if (waitForReceiver) receiverDecision.await() else FileOfferDecision.Accepted(manifest.map { it.id }.toSet())
        }
        val acceptResult = CompletableDeferred<ConnectionResponse>()
        val declineResult = CompletableDeferred<ConnectionResponse>()
        var acceptedEndpoint: String? = null
        var declinedEndpoint: String? = null
        var connectedEndpoint: String? = null
        var sharedFiles: List<SelectedFile> = emptyList()

        override suspend fun accept(endpointId: String): ConnectionResponse {
            acceptedEndpoint = endpointId
            return acceptResult.await()
        }

        override suspend fun decline(endpointId: String): ConnectionResponse {
            declinedEndpoint = endpointId
            return declineResult.await()
        }

        val events = Channel<FileTransferEvent>(Channel.UNLIMITED)
        val cancelResult = CompletableDeferred<Boolean>()
        var onFinishSession: () -> Unit = {}

        override fun finishSession() = onFinishSession()

        override fun transfer(endpointId: String, files: Map<String, SelectedFile>): Flow<FileTransferEvent> = flow {
            connectedEndpoint = endpointId
            sharedFiles = files.values.toList()
            emitAll(events.receiveAsFlow())
        }

        override suspend fun cancelTransfer(endpointId: String): Boolean = cancelResult.await()
    }
}
