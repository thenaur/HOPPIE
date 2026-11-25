package com.example.hoppie.features.receive

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hoppie.data.nearby.NearbyRole
import com.example.hoppie.data.nearby.Peer
import com.example.hoppie.data.nearby.Phase
import com.example.hoppie.data.nearby.SessionState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.hoppie.core.components.HoppieNotice
import com.example.hoppie.core.components.OfferedFile
import com.example.hoppie.core.theme.HOPPIETheme
import com.example.hoppie.features.send.FileTransferEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomingFilesScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun manifestIsReviewedBeforeAcknowledgedAcceptanceStartsTransfer() {
        val model = showReceiver()
        val actions = TestActions()
        val offer = offer()
        compose.onNodeWithText("Incoming Files").assertDoesNotExist()
        compose.runOnIdle { assertTrue(model.onIncomingFileOffer(offer, actions)) }

        compose.onNodeWithText("Incoming Files").assertIsDisplayed()
        compose.onNodeWithText(offer.senderName).assertIsDisplayed()
        compose.onNodeWithText("2 files • 64 MB").assertIsDisplayed()
        compose.onNodeWithText(offer.files.first().name).assertIsDisplayed()
        compose.onNodeWithText("Receive Files").performClick()
        compose.onNodeWithText("Preparing…").assertIsNotEnabled()
        compose.onNodeWithText("Decline Files").assertIsNotEnabled()
        compose.onNodeWithText("Transferring Files").assertDoesNotExist()
        compose.waitUntil(timeoutMillis = 5_000) { actions.accepted != null }
        compose.runOnIdle {
            assertEquals(offer, actions.accepted)
            actions.acceptedByProtocol.complete(Unit)
        }
        compose.onNodeWithText("Transferring Files").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(offer.files.map { it.name }, model.state.value.transfer?.files?.map { it.file.name })
            model.onIncomingOfferEnded(offer.sessionId, "Connection lost.")
        }
        compose.onNodeWithText("Failed to transfer files").assertIsDisplayed()
        compose.onNodeWithText("Connection lost.").assertIsDisplayed()
        compose.onNodeWithText("Retry Transfer").assertIsNotEnabled()
    }

    @Test
    fun declineSendsOnlyRejectionAndReturnsToDiscovery() {
        val model = showReceiver()
        val offer = offer()
        val actions = TestActions()
        compose.runOnIdle { assertTrue(model.onIncomingFileOffer(offer, actions)) }

        compose.onNodeWithText("Decline Files").performClick()
        compose.onNodeWithText("Declining…").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(offer, actions.declined)
            assertNull(actions.accepted)
            assertNull(model.state.value.transfer)
            actions.rejectedByProtocol.complete(Unit)
        }
        compose.onNodeWithText("Incoming Files").assertDoesNotExist()
        compose.onNodeWithText("Transferring Files").assertDoesNotExist()
    }

    @Test
    fun duplicateDecisionsExcludeSkippedIdsAndChooseAnUnusedFilename() {
        val model = showReceiver()
        val actions = TestActions()
        val original = offer().files.first().copy(sha256 = "a".repeat(64))
        val copy = original.copy(id = "copy")
        val different = original.copy(id = "different", sha256 = "b".repeat(64), name = "Other.pdf")
        val offer = offer().copy(files = listOf(original, copy, different))
        actions.stored = listOf(
            StoredFile(original.name, original.sizeBytes, original.sha256),
            StoredFile("Incoming document (1).pdf", original.sizeBytes, null),
            StoredFile(different.name.uppercase(), different.sizeBytes, "c".repeat(64))
        )
        compose.runOnIdle { assertTrue(model.onIncomingFileOffer(offer, actions)) }
        compose.onNodeWithText("Receive Files").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { model.state.value.incoming?.reviewingDuplicates == true }
        compose.onNodeWithText("Duplicate Files").assertIsDisplayed()
        compose.onNodeWithText("“${original.name}” is duplicate.").assertIsDisplayed()
        compose.onNodeWithText(original.name).assertIsDisplayed()
        compose.onNodeWithText("Skip Transfer").performClick()
        compose.runOnIdle {
            assertEquals(copy.id, model.state.value.incoming?.duplicateReview?.remaining?.first()?.id)
            assertNull(actions.accepted)
            model.decideDuplicate(original.id, skip = true)
        }
        compose.onNodeWithText("Receive Anyway").performClick()
        compose.runOnIdle {
            assertEquals(
                mapOf(copy.id to "Incoming document (2).pdf", different.id to "Other (1).pdf"),
                actions.destinations
            )
            actions.acceptedByProtocol.complete(Unit)
        }
        compose.onNodeWithText("Transferring Files").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(2, model.state.value.transfer?.files?.size)
            model.onIncomingOfferEnded(offer.sessionId, "Connection lost.")
        }
    }

    @Test
    fun compactReceiverExposesSessionActionsAndComparesThePromptCode() {
        val searching = SessionState(role = NearbyRole.Discoverer, phase = Phase.Searching)
        val prompt = searching.copy(phase = Phase.Verify, peer = Peer("peer", "Test sender"), digits = "0359")
        val session = mutableStateOf(searching)
        val model = ReceiveViewModel()
        val stopped = mutableListOf<Phase>()
        var decision: Pair<SessionState, Boolean>? = null
        compose.setContent {
            HOPPIETheme {
                Box(Modifier.size(320.dp, 480.dp)) {
                    ReceiveScreen(
                        onBack = {}, model = model, nearby = session.value,
                        onStop = { stopped.add(session.value.phase) },
                        onDecision = { request, accept -> decision = request to accept }
                    )
                }
            }
        }
        compose.onNodeWithText("Stop").assertIsDisplayed().performClick()
        compose.runOnIdle { session.value = prompt }
        compose.onNodeWithText("Test sender").assertIsDisplayed()
        compose.onNodeWithText("Compare this code on both phones").assertIsDisplayed()
        compose.onNodeWithText(prompt.digits).assertIsDisplayed()
        for (accept in listOf(false, true)) {
            compose.onNodeWithText(if (accept) "Accept" else "Decline Request").performClick()
            compose.runOnIdle { assertEquals(prompt to accept, decision) }
        }
        compose.runOnIdle { session.value = prompt.copy(phase = Phase.Connected) }
        compose.onNodeWithText("Disconnect").assertIsDisplayed().performClick()
        val actions = TestActions()
        compose.runOnIdle { assertTrue(model.onIncomingFileOffer(offer(), actions)) }
        compose.onNodeWithText("Incoming Files").assertIsDisplayed()
        compose.onNodeWithText("Disconnect").assertDoesNotExist()
        compose.onNodeWithText("Receive Files").performClick()
        compose.runOnIdle { actions.acceptedByProtocol.complete(Unit) }
        compose.waitUntil(5_000) { model.state.value.transfer != null }
        compose.onNodeWithText("Transferring Files").assertIsDisplayed()
        compose.onNodeWithText("Disconnect").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(Phase.Searching, Phase.Connected), stopped) }
    }

    @Test
    fun completedReceiptWaitsForDoneAndDoesNotStartAnotherSearch() {
        val model = ReceiveViewModel()
        val actions = TestActions()
        val offer = offer()
        var scans = 0
        var completions = 0
        compose.setContent {
            HOPPIETheme {
                ReceiveScreen(onBack = {}, model = model, onScan = { scans++ }, onTransferDone = { completions++ })
            }
        }
        compose.runOnIdle { assertTrue(model.onIncomingFileOffer(offer, actions)) }
        compose.onNodeWithText("Receive Files").performClick()
        compose.runOnIdle {
            actions.events = flow {
                for (file in offer.files) emit(FileTransferEvent.Verified(file.asTransferFile().uri))
            }
            actions.acceptedByProtocol.complete(Unit)
        }
        compose.waitUntil(5_000) { model.state.value.transfer?.active == false }
        compose.onNodeWithText("Transfer Complete!").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Done").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, scans)
            assertEquals(1, completions)
            assertNull(model.state.value.transfer)
        }
    }

    @Test
    fun receiverDiscoveryTracksConnectingVerifyAwaitingAndConnectedWithoutSessionActive() {
        val peer = Peer("p1", "CPH2641")
        val searching = SessionState(
            role = NearbyRole.Discoverer,
            phase = Phase.Searching,
            endpoints = mapOf(peer.id to peer)
        )
        val model = ReceiveViewModel()
        val session = mutableStateOf(searching)
        compose.setContent {
            HOPPIETheme {
                ReceiveScreen(onBack = {}, model = model, nearby = session.value)
            }
        }
        compose.onNodeWithText("Connect").assertIsDisplayed()

        compose.runOnIdle {
            session.value = searching.copy(phase = Phase.Requesting, peer = peer)
        }
        compose.onNodeWithText("Connecting to CPH2641").assertIsDisplayed()
        compose.onNodeWithText("Connecting...").assertIsDisplayed()
        compose.onNodeWithText("Nearby session active").assertDoesNotExist()

        compose.runOnIdle {
            session.value = searching.copy(
                phase = Phase.Verify,
                peer = peer,
                digits = "0456",
                endpoints = emptyMap()
            )
        }
        compose.onNodeWithText("Confirm connection with CPH2641").assertIsDisplayed()
        compose.onNodeWithText("Compare this code on both phones").assertIsDisplayed()
        compose.onNodeWithText("Nearby session active").assertDoesNotExist()

        compose.runOnIdle {
            session.value = searching.copy(
                phase = Phase.AwaitingConnectionResult,
                peer = peer,
                digits = "0456",
                endpoints = emptyMap()
            )
        }
        compose.onNodeWithText("Connecting to CPH2641").assertIsDisplayed()
        compose.onNodeWithText("Waiting for confirmation on the other phone.").assertIsDisplayed()
        compose.onNodeWithText("Found Nearby (1)").assertIsDisplayed()
        compose.onNodeWithText("Connecting...").assertIsDisplayed()
        compose.onNodeWithText("Nearby session active").assertDoesNotExist()

        compose.runOnIdle {
            session.value = searching.copy(
                phase = Phase.Connected,
                peer = peer,
                digits = "",
                endpoints = emptyMap()
            )
        }
        compose.onNodeWithText("Connected to CPH2641").assertIsDisplayed()
        compose.onNodeWithText("Waiting for the sender to share files.").assertIsDisplayed()
        compose.onNodeWithText("Found Nearby (1)").assertIsDisplayed()
        compose.onNodeWithText("Connected").assertIsDisplayed()
        compose.onNodeWithText("Disconnect").assertIsDisplayed()
        compose.onNodeWithText("Nearby session active").assertDoesNotExist()
    }

    @Test
    fun completedReceiptDoneStopsConnectedSessionAndIsIdempotent() {
        val model = ReceiveViewModel()
        val actions = TestActions()
        val offer = offer()
        val connected = SessionState(role = NearbyRole.Discoverer, phase = Phase.Connected, peer = Peer("p1", "Sender"))
        var stops = 0
        var completions = 0
        compose.setContent {
            HOPPIETheme {
                ReceiveScreen(
                    onBack = {},
                    model = model,
                    nearby = connected,
                    onStop = { stops++ },
                    onTransferDone = { completions++ }
                )
            }
        }
        compose.runOnIdle { assertTrue(model.onIncomingFileOffer(offer, actions)) }
        compose.onNodeWithText("Receive Files").performClick()
        compose.runOnIdle {
            actions.events = flow {
                for (file in offer.files) emit(FileTransferEvent.Verified(file.asTransferFile().uri))
            }
            actions.acceptedByProtocol.complete(Unit)
        }
        compose.waitUntil(5_000) { model.state.value.transfer?.active == false }
        compose.onNodeWithText("Done").assertIsDisplayed().performClick()
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle {
            assertEquals(1, stops)
            assertEquals(1, completions)
            assertNull(model.state.value.transfer)
        }
    }

    @Test
    fun stopWhileConnectingShowsConnectionCancelledAndAllowsRescan() {
        val peer = Peer("p1", "CPH2641")
        val session = mutableStateOf(
            SessionState(
                role = NearbyRole.Discoverer,
                phase = Phase.Requesting,
                peer = peer,
                endpoints = mapOf(peer.id to peer)
            )
        )
        val model = ReceiveViewModel()
        var scans = 0
        compose.setContent {
            HOPPIETheme {
                ReceiveScreen(
                    onBack = {},
                    model = model,
                    nearby = session.value,
                    onScan = if (session.value.phase == Phase.Idle) {
                        {
                            scans++
                            session.value = SessionState(role = NearbyRole.Discoverer, phase = Phase.Starting)
                        }
                    } else null,
                    onStop = {
                        session.value = SessionState(phase = Phase.Idle, connectionCancelled = true)
                    }
                )
            }
        }

        compose.onNodeWithText("Connecting to CPH2641").assertIsDisplayed()
        compose.onNodeWithText("Stop").assertIsDisplayed().performClick()

        compose.onNodeWithText("Connection cancelled").assertIsDisplayed()
        compose.onNodeWithText("Tap Rescan to search for nearby devices.").assertIsDisplayed()
        compose.onNodeWithText("Found Nearby (0)").assertIsDisplayed()
        compose.onNodeWithText("Device search completed").assertDoesNotExist()

        compose.onNodeWithText("Rescan").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, scans)
            assertEquals(Phase.Starting, session.value.phase)
        }
    }

    @Test
    fun receiverShowsCustomNotificationCardsWithoutBlockingActions() {
        val model = ReceiveViewModel()
        val session = mutableStateOf(
            SessionState(
                phase = Phase.Idle,
                cardNotice = HoppieNotice.ConnectionDeclined("Haribon's Phone")
            )
        )
        var dismissed = 0
        compose.setContent {
            HOPPIETheme {
                ReceiveScreen(
                    onBack = {},
                    model = model,
                    nearby = session.value,
                    onScan = {},
                    onDismissNotice = {
                        dismissed++
                        session.value = session.value.copy(cardNotice = null, notice = null)
                    }
                )
            }
        }
        compose.onNodeWithTag("hoppie_notice_connection_declined").assertIsDisplayed()
        compose.onNodeWithText("Haribon's Phone").assertIsDisplayed()
        compose.onNodeWithText("has declined request to connect").assertIsDisplayed()
        compose.onNodeWithText("Rescan").assertIsEnabled()

        compose.runOnIdle {
            session.value = SessionState(phase = Phase.Idle, cardNotice = HoppieNotice.ConnectionLost())
        }
        compose.onNodeWithTag("hoppie_notice_connection_lost").assertIsDisplayed()
        compose.onNodeWithText("Connection Lost").assertIsDisplayed()
        compose.onNodeWithText("Your transfer was interrupted.\nOnly verified files are available.").assertIsDisplayed()
        compose.onNodeWithText("Your progress is saved", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Dismiss notification").performClick()
        compose.runOnIdle {
            assertEquals(1, dismissed)
        }
        compose.onNodeWithTag("hoppie_notice_connection_lost").assertDoesNotExist()

        val actions = TestActions()
        val offer = offer()
        compose.runOnIdle {
            assertTrue(model.onIncomingFileOffer(offer, actions))
            model.onIncomingOfferEnded(offer.sessionId, "Disconnected.")
        }
        compose.onNodeWithTag("hoppie_notice_connection_lost").assertIsDisplayed()
    }

    private fun showReceiver(): ReceiveViewModel {
        val model = ReceiveViewModel()
        compose.setContent { HOPPIETheme { ReceiveScreen(onBack = {}, model = model) } }
        return model
    }

    private fun offer() = IncomingFileOffer(
        sessionId = "test-session",
        senderName = "Test sender’s phone",
        files = listOf(
            OfferedFile("pdf", "Incoming document.pdf", 32L * 1024 * 1024, "application/pdf"),
            OfferedFile("image", "Incoming image.png", 32L * 1024 * 1024, "image/png")
        )
    )

    private class TestActions : IncomingFilesActions {
        var events: Flow<FileTransferEvent> = flow { awaitCancellation() }
        val acceptedByProtocol = CompletableDeferred<Unit>()
        val rejectedByProtocol = CompletableDeferred<Unit>()
        var accepted: IncomingFileOffer? = null
        var declined: IncomingFileOffer? = null

        var stored: List<StoredFile> = emptyList()
        var destinations: Map<String, String> = emptyMap()

        override suspend fun inspectDestination(offer: IncomingFileOffer): List<StoredFile> = stored

        override suspend fun accept(
            offer: IncomingFileOffer,
            destinations: Map<String, String>
        ): Flow<FileTransferEvent> {
            this.destinations = destinations
            accepted = offer
            acceptedByProtocol.await()
            return events
        }

        override suspend fun decline(offer: IncomingFileOffer) {
            declined = offer
            rejectedByProtocol.await()
        }

        override suspend fun cancelTransfer(offer: IncomingFileOffer): Boolean = false
    }
}
