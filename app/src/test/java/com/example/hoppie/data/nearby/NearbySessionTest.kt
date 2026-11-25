package com.example.hoppie.data.nearby

import com.example.hoppie.core.components.HoppieNotice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbySessionTest {
    private val radio = FakeTransport()
    private val logs = mutableListOf<String>()
    private val session = NearbySession(radio, logs::add)
    private val state get() = session.state.value
    private val peer = Peer("endpoint", "Sender")

    @Test fun discoveryTracksUniqueEndpointsAndRequestsOnlyTheSelectedLiveEndpoint() {
        session.start(NearbyRole.Discoverer)
        session.start(NearbyRole.Discoverer)
        assertEquals(listOf(NearbyRole.Discoverer), radio.starts)
        assertEquals(Phase.Starting, state.phase)
        radio.events.started()
        assertEquals(Phase.Searching, state.phase)
        radio.events.found(peer)
        radio.events.found(peer.copy(name = "Renamed"))
        assertEquals(listOf("Renamed"), state.endpoints.values.map { it.name })
        radio.events.lost(peer.id)
        session.request(peer.id)
        assertTrue(radio.requests.isEmpty())
        radio.events.found(peer)
        session.request(peer.id)
        session.request(peer.id)
        assertEquals(listOf(peer.id), radio.requests)
        assertEquals(Phase.Requesting, state.phase)
        radio.events.initiated(peer, "0017")
        assertEquals(Phase.Verify, state.phase)
        assertEquals("0017", state.digits)
        assertTrue(radio.accepts.isEmpty())
        assertEquals(listOf(NearbyRole.Discoverer), radio.stops)
    }

    @Test fun advertisingRequiresExplicitConsentAndNearbySuccess() {
        session.start(NearbyRole.Advertiser)
        radio.events.started()
        assertEquals(Phase.Searching, state.phase)
        radio.events.initiated(peer, "0123")
        val prompt = state
        radio.events.initiated(peer, "0123")
        radio.events.initiated(Peer("other", "Other"), "9999")
        assertEquals(listOf("other"), radio.rejects)
        assertEquals(prompt, state)
        assertEquals(listOf(NearbyRole.Advertiser), radio.stops)
        assertTrue(radio.accepts.isEmpty())
        session.decide(prompt, true)
        session.decide(prompt, true)
        assertEquals(listOf(peer.id), radio.accepts)
        assertEquals(Phase.AwaitingConnectionResult, state.phase)
        radio.events.result("other", null)
        assertEquals(Phase.AwaitingConnectionResult, state.phase)
        radio.events.result(peer.id, null)
        assertEquals(Phase.Connected, state.phase)
        radio.events.disconnected(peer.id)
        assertEquals(Phase.Idle, state.phase)
        assertEquals(HoppieNotice.ConnectionLost(), state.cardNotice)
        assertEquals(HoppieNotice.ConnectionLost().message, state.notice)
    }

    @Test fun localAndRemoteDeclinesEndTheSession() {
        session.start(NearbyRole.Advertiser)
        radio.events.initiated(peer, "1024")
        session.decide(state, false)
        assertEquals(listOf(peer.id), radio.rejects)
        assertEquals(Phase.Declining, state.phase)
        radio.rejected(null)
        assertEquals("Connection declined.", state.notice)
        assertEquals(HoppieNotice.Info("Connection declined"), state.cardNotice)

        session.start(NearbyRole.Advertiser)
        radio.events.initiated(peer, "2048")
        session.decide(state, true)
        radio.events.rejected(peer.id)
        assertEquals(HoppieNotice.ConnectionDeclined(peer.name), state.cardNotice)
        assertEquals("Sender has declined request to connect", state.notice)
        assertEquals(Phase.Idle, state.phase)

        session.start(NearbyRole.Discoverer)
        radio.events.started()
        radio.events.found(peer)
        session.request(peer.id)
        radio.events.initiated(peer, "3072")
        session.decide(state, true)
        radio.events.result(peer.id, "Connection declined.")
        assertEquals(HoppieNotice.ConnectionDeclined(peer.name), state.cardNotice)
        assertEquals(Phase.Idle, state.phase)
    }

    @Test fun staleCallbacksAndOldConfirmationCannotAffectANewSession() {
        session.start(NearbyRole.Advertiser)
        val old = radio.events
        old.initiated(peer, "1234")
        val oldPrompt = state
        session.stop()
        assertEquals(1, radio.closes)
        session.start(NearbyRole.Advertiser)
        radio.events.initiated(peer, "1234")
        old.result(peer.id, null)
        old.failed("obsolete")
        old.disconnected(peer.id)
        session.decide(oldPrompt, true)
        assertEquals(Phase.Verify, state.phase)
        assertTrue(radio.accepts.isEmpty())
        assertTrue(logs.any { "result outcome=success" in it && "ignored=stale_callback" in it })
        assertFalse(logs.any { "1234" in it || peer.name in it })
    }

    @Test fun missingDigitsAndUnsolicitedSuccessFailClosed() {
        session.start(NearbyRole.Advertiser)
        radio.events.initiated(peer, "")
        assertEquals(listOf(peer.id), radio.rejects)
        radio.rejected(null)
        assertEquals(Phase.Idle, state.phase)
        session.start(NearbyRole.Advertiser)
        radio.events.initiated(peer, "2222")
        radio.events.result(peer.id, null)
        assertEquals(Phase.Idle, state.phase)
        assertTrue(radio.accepts.isEmpty())
    }

    @Test fun transportFailuresClearEndpointsAndPermitRetry() {
        session.start(NearbyRole.Discoverer)
        radio.events.failed("Play services unavailable")
        assertEquals("Play services unavailable", state.notice)
        session.start(NearbyRole.Discoverer)
        radio.events.started()
        radio.events.found(peer)
        session.request(peer.id)
        radio.events.failed("Request failed")
        assertEquals("Request failed", state.notice)
        assertTrue(state.endpoints.isEmpty())
        assertEquals(Phase.Idle, state.phase)
    }

    private fun acceptLocally(session: NearbySession, transport: FakeTransport, role: NearbyRole, remote: Peer) {
        session.start(role)
        transport.events.started()
        if (role == NearbyRole.Discoverer) {
            transport.events.found(remote)
            session.request(remote.id)
        }
        transport.events.initiated(remote, "0042")
        session.decide(session.state.value, true)
        assertEquals(listOf(remote.id), transport.accepts)
        assertEquals(Phase.AwaitingConnectionResult, session.state.value.phase)
    }

    @Test fun bothPeersNeedTheirOwnSuccessfulResultAfterAcceptanceTaskSuccess() {
        val otherRadio = FakeTransport()
        val otherSession = NearbySession(otherRadio) {}
        val otherPeer = Peer("receiver", "Receiver")
        acceptLocally(session, radio, NearbyRole.Advertiser, otherPeer)
        acceptLocally(otherSession, otherRadio, NearbyRole.Discoverer, peer)

        radio.completeAccept(AcceptOutcome.Succeeded)
        otherRadio.completeAccept(AcceptOutcome.Succeeded)
        assertEquals(Phase.AwaitingConnectionResult, state.phase)
        assertEquals(Phase.AwaitingConnectionResult, otherSession.state.value.phase)

        radio.events.result(otherPeer.id, null)
        assertEquals(Phase.Connected, state.phase)
        assertEquals(Phase.AwaitingConnectionResult, otherSession.state.value.phase)
        otherRadio.events.result(peer.id, null)
        assertEquals(Phase.Connected, otherSession.state.value.phase)
    }

    @Test fun acceptanceTaskFailureClosesTheSessionAndSurfacesItsStatus() {
        acceptLocally(session, radio, NearbyRole.Advertiser, peer)
        radio.completeAccept(AcceptOutcome.Failed, status = 17)
        assertEquals(Phase.Idle, state.phase)
        assertEquals("Nearby could not accept the connection (status 17).", state.notice)
        assertEquals(1, radio.closes)
    }

    @Test fun cancelledAcceptanceTaskLeavesWaitingAndIgnoresLateSuccess() {
        acceptLocally(session, radio, NearbyRole.Discoverer, peer)
        radio.completeAccept(AcceptOutcome.Cancelled)
        radio.events.result(peer.id, null)
        assertEquals(Phase.Idle, state.phase)
        assertEquals("Nearby cancelled the acceptance request.", state.notice)
    }

    @Test fun unsuccessfulConnectionResultAfterSuccessfulTaskEndsTheSession() {
        acceptLocally(session, radio, NearbyRole.Discoverer, peer)
        radio.completeAccept(AcceptOutcome.Succeeded)
        radio.events.result(peer.id, "Connection failed (STATUS_ERROR).")
        assertEquals(Phase.Idle, state.phase)
        assertEquals("Connection failed (STATUS_ERROR).", state.notice)
        assertEquals(HoppieNotice.ConnectionLost(), state.cardNotice)
    }

    @Test fun oldTaskAndWrongEndpointCannotCompleteOrCancelANewAcceptance() {
        acceptLocally(session, radio, NearbyRole.Advertiser, peer)
        val old = radio.acceptEvents
        session.stop()
        radio.accepts.clear()
        acceptLocally(session, radio, NearbyRole.Advertiser, peer)
        old.acceptCompleted(peer.id, AcceptOutcome.Cancelled, null)
        radio.events.result("other", null)
        assertTrue(logs.any { "acceptTask" in it && "ignored=stale_callback" in it })
        assertTrue(logs.any { "result" in it && "ignored=mismatched_endpoint" in it })
        assertEquals(Phase.AwaitingConnectionResult, state.phase)
    }

    @Test fun completionClosesOnlyTheAuthenticatedPeerAndGeneration() {
        acceptLocally(session, radio, NearbyRole.Advertiser, peer)
        val old = radio.events
        old.exchangeCompleted(peer.id)
        assertEquals(Phase.AwaitingConnectionResult, state.phase)
        old.result(peer.id, null)
        old.exchangeCompleted("other")
        assertEquals(Phase.Connected, state.phase)
        old.exchangeCompleted(peer.id)
        assertEquals(Phase.Idle, state.phase)
        assertEquals(null, state.notice)
        old.exchangeCompleted(peer.id)
        assertEquals(1, radio.closes)
        radio.accepts.clear()
        acceptLocally(session, radio, NearbyRole.Advertiser, peer)
        radio.events.result(peer.id, null)
        old.exchangeCompleted(peer.id)
        assertEquals(Phase.Connected, state.phase)
    }

    @Test fun stopFromConnectedEndsSilentlyAndIgnoresSubsequentDisconnect() {
        acceptLocally(session, radio, NearbyRole.Discoverer, peer)
        radio.events.result(peer.id, null)
        assertEquals(Phase.Connected, state.phase)

        session.stop()
        assertEquals(Phase.Idle, state.phase)
        assertEquals(null, state.notice)
        assertEquals(1, radio.closes)

        radio.events.disconnected(peer.id)
        assertEquals(Phase.Idle, state.phase)
        assertEquals(null, state.notice)
    }

    @Test fun disconnectAndResultErrorDuringLocalDeclineFinishWithDeclinedNotice() {
        session.start(NearbyRole.Advertiser)
        radio.events.initiated(peer, "1024")
        session.decide(state, false)
        assertEquals(Phase.Declining, state.phase)

        radio.events.disconnected(peer.id)
        assertEquals(Phase.Idle, state.phase)
        assertEquals("Connection declined.", state.notice)
        assertEquals(HoppieNotice.Info("Connection declined"), state.cardNotice)
        assertEquals(1, radio.closes)

        radio.rejected("late error")
        assertEquals("Connection declined.", state.notice)

        session.start(NearbyRole.Discoverer)
        radio.events.started()
        radio.events.found(peer)
        session.request(peer.id)
        radio.events.initiated(peer, "4096")
        session.decide(state, false)
        radio.events.result(peer.id, "Connection error")
        assertEquals(Phase.Idle, state.phase)
        assertEquals(HoppieNotice.Info("Connection declined"), state.cardNotice)
    }

    @Test fun clearNoticeClearsIdleNoticeWithoutClosingTransport() {
        acceptLocally(session, radio, NearbyRole.Advertiser, peer)
        radio.events.result(peer.id, null)
        radio.events.connectionLost(peer.id)
        assertEquals(HoppieNotice.ConnectionLost().message, state.notice)
        assertEquals(HoppieNotice.ConnectionLost(), state.cardNotice)

        val closesBefore = radio.closes
        session.clearNotice()
        assertEquals(Phase.Idle, state.phase)
        assertNull(state.notice)
        assertNull(state.cardNotice)
        assertFalse(state.connectionCancelled)
        assertEquals(closesBefore, radio.closes)
    }

    @Test fun stopWhileConnectingMarksConnectionCancelledAndAllowsRestart() {
        session.start(NearbyRole.Discoverer)
        radio.events.started()
        radio.events.found(peer)
        session.request(peer.id)
        assertEquals(Phase.Requesting, state.phase)

        session.stop()
        assertEquals(Phase.Idle, state.phase)
        assertTrue(state.connectionCancelled)
        assertEquals(null, state.notice)

        session.start(NearbyRole.Discoverer)
        assertEquals(Phase.Starting, state.phase)
        assertFalse(state.connectionCancelled)

        session.stop()
        assertFalse(state.connectionCancelled)
        assertEquals(null, state.notice)

        session.start(NearbyRole.Advertiser)
        radio.events.initiated(peer, "0987")
        session.stop()
        assertEquals(Phase.Idle, state.phase)
        assertFalse(state.connectionCancelled)
        assertEquals("Connection cancelled.", state.notice)
    }

    @Test fun runtimePermissionsFollowPlatformBoundaries() {
        fun names(sdk: Int) = nearbyPermissions(sdk).map { it.substringAfterLast('.') }.toSet()
        val coarse = setOf("ACCESS_COARSE_LOCATION")
        val location = coarse + "ACCESS_FINE_LOCATION"
        val bluetooth = setOf("BLUETOOTH_SCAN", "BLUETOOTH_ADVERTISE", "BLUETOOTH_CONNECT")
        assertEquals(coarse, names(24))
        assertEquals(location, names(29))
        assertEquals(location + bluetooth, names(31))
        assertEquals(location + bluetooth, names(32))
        assertEquals(bluetooth + "NEARBY_WIFI_DEVICES", names(33))
        assertFalse("ACCESS_LOCAL_NETWORK" in names(36))
        assertEquals(names(33) + "ACCESS_LOCAL_NETWORK", names(37))
    }

    private class FakeTransport : NearbyTransport {
        lateinit var events: NearbyTransport.Events
        lateinit var rejected: (String?) -> Unit
        val starts = mutableListOf<NearbyRole>()
        val stops = mutableListOf<NearbyRole>()
        val requests = mutableListOf<String>()
        val accepts = mutableListOf<String>()
        val rejects = mutableListOf<String>()
        var closes = 0
        override fun start(role: NearbyRole, events: NearbyTransport.Events) {
            starts += role
            this.events = events
        }
        override fun request(id: String, events: NearbyTransport.Events) { requests += id }
        lateinit var acceptEvents: NearbyTransport.Events
        override fun accept(id: String, events: NearbyTransport.Events) {
            accepts += id
            acceptEvents = events
        }
        fun completeAccept(outcome: AcceptOutcome, status: Int? = null) {
            acceptEvents.acceptCompleted(accepts.last(), outcome, status)
        }
        override fun reject(id: String, onComplete: (String?) -> Unit) {
            rejects += id
            rejected = onComplete
        }
        override fun stopSearch(role: NearbyRole) { stops += role }
        override fun close() { closes++ }
    }
}
