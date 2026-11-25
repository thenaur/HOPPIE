package com.example.hoppie.data.nearby

import com.example.hoppie.core.components.HoppieNotice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class NearbyRole { Advertiser, Discoverer }
internal enum class AcceptOutcome { Succeeded, Failed, Cancelled }
internal enum class Phase { Idle, Starting, Searching, Requesting, Verify, AwaitingConnectionResult, Declining, Connected }
internal data class Peer(val id: String, val name: String)
internal data class SessionState(
    val generation: Long = 0,
    val role: NearbyRole? = null,
    val phase: Phase = Phase.Idle,
    val endpoints: Map<String, Peer> = emptyMap(),
    val peer: Peer? = null,
    val digits: String = "",
    val notice: String? = null,
    val cardNotice: HoppieNotice? = null,
    val connectionCancelled: Boolean = false
)

/** All calls and callbacks run on the main thread. Each start gets a fresh callback instance. */
internal interface NearbyTransport {
    interface Events {
        fun acceptCompleted(id: String, outcome: AcceptOutcome, status: Int?)
        fun started()
        fun failed(message: String)
        fun found(peer: Peer)
        fun lost(id: String)
        fun initiated(peer: Peer, digits: String)
        fun result(id: String, error: String?)
        fun rejected(id: String) = result(id, "Connection declined.")
        fun connectionLost(id: String) = disconnected(id)
        fun exchangeCompleted(id: String)
        fun disconnected(id: String)
    }

    fun start(role: NearbyRole, events: Events)
    fun request(id: String, events: Events)
    fun accept(id: String, events: Events)
    fun reject(id: String, onComplete: (String?) -> Unit)
    fun stopSearch(role: NearbyRole)
    fun close()
}

internal class NearbySession(
    private val transport: NearbyTransport,
    private val log: (String) -> Unit = ::logNearby
) {
    private val mutable = MutableStateFlow(SessionState())
    val state = mutable.asStateFlow()
    private val current get() = mutable.value
    private var events: Events? = null

    private fun trace(event: String, endpoint: String? = null) {
        log("session $event generation=${current.generation} role=${current.role} phase=${current.phase} " +
            "endpoint=$endpoint activeEndpoint=${current.peer?.id}")
    }

    private fun transition(next: SessionState, event: String) {
        trace("$event transition=${current.phase}->${next.phase} nextGeneration=${next.generation} nextRole=${next.role}", next.peer?.id)
        mutable.value = next
    }

    fun start(role: NearbyRole) {
        if (current.phase != Phase.Idle) return
        transition(SessionState(generation = current.generation + 1, role = role, phase = Phase.Starting), "start")
        val callbacks = Events()
        events = callbacks
        transport.start(role, callbacks)
    }

    fun request(id: String) {
        val peer = current.endpoints[id] ?: return
        if (current.role != NearbyRole.Discoverer || current.phase != Phase.Searching) return
        transition(current.copy(phase = Phase.Requesting, peer = peer), "request")
        transport.request(id, checkNotNull(events))
    }

    fun decide(prompt: SessionState, accept: Boolean) {
        if (current.generation != prompt.generation || current.phase != Phase.Verify ||
            current.peer != prompt.peer || current.digits != prompt.digits) {
            trace("decision ignored=obsolete_prompt promptGeneration=${prompt.generation}", prompt.peer?.id)
            return
        }
        val id = checkNotNull(prompt.peer).id
        val callbacks = checkNotNull(events)
        if (accept) {
            transition(current.copy(phase = Phase.AwaitingConnectionResult), "localAccept")
            transport.accept(id, callbacks)
        } else {
            transition(current.copy(phase = Phase.Declining), "localDecline")
            transport.reject(id) { error ->
                if (error == null) callbacks.rejected(id)
                else callbacks.failed(error)
            }
        }
    }

    fun stop() {
        val wasConnecting = current.phase in connectingPhases
        val senderCancel = wasConnecting && current.role == NearbyRole.Advertiser
        finish(
            message = if (senderCancel) "Connection cancelled." else null,
            cardNotice = if (senderCancel) HoppieNotice.Info("Connection cancelled", "Tap Send when you are ready to share again.") else null,
            connectionCancelled = wasConnecting && current.role == NearbyRole.Discoverer
        )
    }

    fun clearNotice() {
        if (current.phase == Phase.Idle &&
            (current.notice != null || current.cardNotice != null || current.connectionCancelled)
        ) {
            mutable.value = current.copy(notice = null, cardNotice = null, connectionCancelled = false)
        }
    }

    fun permissionDenied() {
        if (current.phase == Phase.Idle) {
            val detail = "Allow the requested Nearby permissions to find and connect to devices. If the prompt no longer appears, open Settings → Apps → HOPPIE → Permissions."
            mutable.value = current.copy(
                notice = detail,
                cardNotice = HoppieNotice.Info("Permissions required", detail)
            )
        }
    }

    private fun finish(
        message: String?,
        cardNotice: HoppieNotice? = null,
        connectionCancelled: Boolean = false
    ) {
        val hadSession = events != null
        events = null
        transition(
            SessionState(
                generation = current.generation,
                notice = message,
                cardNotice = cardNotice,
                connectionCancelled = connectionCancelled
            ),
            "finish"
        )
        if (hadSession) transport.close()
    }

    private inner class Events : NearbyTransport.Events {
        private val generation = current.generation

        private fun receive(event: String, id: String? = null, matchPeer: Boolean = false): Boolean {
            val reason = when {
                events !== this -> "stale_callback"
                matchPeer && current.peer?.id != id -> "mismatched_endpoint"
                else -> null
            }
            trace("$event callbackGeneration=$generation" + (reason?.let { " ignored=$it" } ?: ""), id)
            return reason == null
        }

        override fun acceptCompleted(id: String, outcome: AcceptOutcome, status: Int?) {
            if (!receive("acceptTask outcome=$outcome status=$status", id, matchPeer = true)) return
            when (outcome) {
                AcceptOutcome.Succeeded -> Unit // Nearby still decides the connection in result().
                AcceptOutcome.Cancelled -> finish(
                    "Nearby cancelled the acceptance request.",
                    cardNotice = HoppieNotice.Info("Connection cancelled", "Please try connecting again.")
                )
                AcceptOutcome.Failed -> finish(
                    "Nearby could not accept the connection" + (status?.let { " (status $it)." } ?: "."),
                    cardNotice = HoppieNotice.Info("Could not accept connection", "Please try connecting again.")
                )
            }
        }

        override fun started() {
            if (!receive("started")) return
            if (current.phase == Phase.Starting) transition(current.copy(phase = Phase.Searching), "started")
            else trace("started ignored=already_advanced")
        }

        override fun failed(message: String) {
            if (!receive("failure")) return
            val info = HoppieNotice.Info("Connection unavailable", message)
            finish(message, cardNotice = info)
        }

        override fun found(peer: Peer) {
            if (!receive("found", peer.id)) return
            if (current.role == NearbyRole.Discoverer && current.phase in discoveryPhases) {
                mutable.value = current.copy(endpoints = current.endpoints + (peer.id to peer))
            } else trace("found ignored=not_discovering", peer.id)
        }

        override fun lost(id: String) {
            if (receive("lost", id)) mutable.value = current.copy(endpoints = current.endpoints - id)
        }

        override fun initiated(peer: Peer, digits: String) {
            if (!receive("initiated", peer.id)) return
            if (current.peer?.id == peer.id && current.phase in handshakePhases) {
                trace("initiated ignored=duplicate", peer.id)
                return
            }
            val expected = when (current.role) {
                NearbyRole.Advertiser -> current.phase in discoveryPhases
                NearbyRole.Discoverer -> current.phase == Phase.Requesting && current.peer?.id == peer.id
                null -> false
            }
            if (!expected) {
                trace("initiated rejecting=unexpected_peer_or_phase", peer.id)
                transport.reject(peer.id) {}
                return
            }
            if (digits.isBlank()) {
                transition(current.copy(peer = peer, phase = Phase.Declining), "missingCode")
                transport.reject(peer.id) { failed("Nearby did not provide an authentication code.") }
                return
            }
            transition(current.copy(phase = Phase.Verify, peer = peer, digits = digits, endpoints = emptyMap()), "initiated")
            transport.stopSearch(checkNotNull(current.role))
        }

        override fun rejected(id: String) {
            if (!receive("rejected", id, matchPeer = true)) return
            if (current.phase == Phase.Declining) {
                finish("Connection declined.", cardNotice = HoppieNotice.Info("Connection declined"))
            } else {
                val declined = HoppieNotice.ConnectionDeclined(current.peer?.name.orEmpty())
                finish(declined.message, cardNotice = declined)
            }
        }

        override fun result(id: String, error: String?) {
            val outcome = if (error == null) "success" else "failure"
            if (!receive("result outcome=$outcome", id, matchPeer = true)) return
            when {
                current.phase == Phase.Declining ->
                    finish("Connection declined.", cardNotice = HoppieNotice.Info("Connection declined"))
                error != null -> {
                    val notice = when {
                        error.contains("declined", ignoreCase = true) ->
                            HoppieNotice.ConnectionDeclined(current.peer?.name.orEmpty())
                        current.phase in connectedOrVerifyingPhases ->
                            HoppieNotice.ConnectionLost()
                        else ->
                            HoppieNotice.Info("Could not connect", "Please check Wi-Fi and Bluetooth and try again.")
                    }
                    finish(error, cardNotice = notice)
                }
                current.phase == Phase.AwaitingConnectionResult ->
                    transition(current.copy(phase = Phase.Connected, digits = ""), "result")
                current.phase != Phase.Connected ->
                    finish("Connection ended before authentication was confirmed.", cardNotice = HoppieNotice.ConnectionLost())
                else -> trace("result ignored=duplicate_success", id)
            }
        }

        override fun connectionLost(id: String) = disconnected(id)

        override fun exchangeCompleted(id: String) {
            if (receive("exchangeCompleted", id, matchPeer = true) && current.phase == Phase.Connected) {
                finish(null)
            }
        }

        override fun disconnected(id: String) {
            if (!receive("disconnected", id, matchPeer = true)) return
            if (current.phase == Phase.Declining) {
                finish("Connection declined.", cardNotice = HoppieNotice.Info("Connection declined"))
            } else {
                val lost = HoppieNotice.ConnectionLost()
                finish(lost.message, cardNotice = lost)
            }
        }
    }

    private companion object {
        val discoveryPhases = setOf(Phase.Starting, Phase.Searching)
        val connectingPhases = setOf(Phase.Requesting, Phase.Verify, Phase.AwaitingConnectionResult)
        val connectedOrVerifyingPhases = setOf(Phase.Verify, Phase.AwaitingConnectionResult, Phase.Connected)
        val handshakePhases = setOf(Phase.Verify, Phase.AwaitingConnectionResult, Phase.Declining, Phase.Connected)
    }
}
