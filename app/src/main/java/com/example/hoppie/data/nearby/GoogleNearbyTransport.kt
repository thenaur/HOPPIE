package com.example.hoppie.data.nearby

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.tasks.Task

internal fun logNearby(message: String) {
    Log.d("HOPPIE-Nearby", message)
}

internal class GoogleNearbyTransport(context: Context, private val state: () -> SessionState) : NearbyTransport {
    private val currentLink = MutableStateFlow<PdfLink?>(null)
    val link = currentLink.asStateFlow()
    private val app = context.applicationContext
    private var client: ConnectionsClient? = null
    private var searching: NearbyRole? = null
    private val strategy = Strategy.P2P_POINT_TO_POINT
    private val name = Build.MODEL.ifBlank { "Android device" }

    override fun start(role: NearbyRole, events: NearbyTransport.Events) {
        val availability = GoogleApiAvailability.getInstance()
        val status = availability.isGooglePlayServicesAvailable(app)
        if (status != ConnectionResult.SUCCESS) {
            events.failed("Google Play services is unavailable (${availability.getErrorString(status)}). Install, enable or update it, then retry.")
            return
        }
        task(events::failed) {
            val connections = Nearby.getConnectionsClient(app)
            client = connections
            searching = role
            when (role) {
                NearbyRole.Advertiser -> connections.startAdvertising(name, app.packageName,
                    lifecycle(events), AdvertisingOptions.Builder().setStrategy(strategy).build())
                NearbyRole.Discoverer -> connections.startDiscovery(app.packageName,
                    object : EndpointDiscoveryCallback() {
                        override fun onEndpointFound(id: String, info: DiscoveredEndpointInfo) {
                            if (info.serviceId == app.packageName) events.found(Peer(id, info.endpointName))
                        }
                        override fun onEndpointLost(id: String) = events.lost(id)
                    }, DiscoveryOptions.Builder().setStrategy(strategy).build())
            }.addOnSuccessListener { events.started() }
        }
    }

    private fun lifecycle(events: NearbyTransport.Events) = object : ConnectionLifecycleCallback() {
        private val generation = state().generation

        override fun onConnectionInitiated(id: String, info: ConnectionInfo) =
            events.initiated(Peer(id, info.endpointName), info.authenticationDigits)

        override fun onConnectionResult(id: String, resolution: ConnectionResolution) {
            val status = resolution.status
            val name = ConnectionsStatusCodes.getStatusCodeString(status.statusCode)
            logNearby("sdk onConnectionResult endpoint=$id status=${status.statusCode} name=$name")
            val current = state()
            when (status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> events.result(id, null)
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> events.rejected(id)
                ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR,
                ConnectionsStatusCodes.STATUS_ERROR,
                ConnectionsStatusCodes.STATUS_ENDPOINT_UNKNOWN -> {
                    if (current.phase in setOf(Phase.Verify, Phase.AwaitingConnectionResult, Phase.Connected)) {
                        events.connectionLost(id)
                    } else {
                        events.result(id, "Could not connect. Please try again.")
                    }
                }
                else -> events.result(id, "Could not connect. Please try again.")
            }
        }

        override fun onDisconnected(id: String) {
            logNearby("sdk onDisconnected endpoint=$id")
            val current = state()
            val receipt = currentLink.value?.finalReceipt?.takeIf {
                current.generation == generation && current.peer?.id == id
            }
            if (receipt == null) events.disconnected(id)
            else receipt.onDisconnect { delivered ->
                if (delivered) events.exchangeCompleted(id) else events.disconnected(id)
            }
        }
    }

    override fun request(id: String, events: NearbyTransport.Events) {
        task(events::failed) { checkNotNull(client).requestConnection(name, id, lifecycle(events)) }
    }

    override fun accept(id: String, events: NearbyTransport.Events) {
        val prompt = state()
        val payloadCallback = PdfLink(app, checkNotNull(client), checkNotNull(prompt.peer), checkNotNull(prompt.role),
            finishSession = { events.exchangeCompleted(id) }) {
            val current = state()
            current.generation == prompt.generation && current.phase == Phase.Connected && current.peer?.id == id
        }
        currentLink.value = payloadCallback

        logNearby("sdk acceptConnection endpoint=$id")
        try {
            checkNotNull(client).acceptConnection(id, payloadCallback).addOnCompleteListener { task ->
                val outcome = when {
                    task.isCanceled -> AcceptOutcome.Cancelled
                    task.isSuccessful -> AcceptOutcome.Succeeded
                    else -> AcceptOutcome.Failed
                }
                val failure = task.exception
                val status = (failure as? ApiException)?.statusCode
                logNearby("sdk acceptTask endpoint=$id outcome=$outcome exception=${failure?.javaClass?.simpleName} status=$status")
                events.acceptCompleted(id, outcome, status)
            }
        } catch (_: SecurityException) {
            logNearby("sdk acceptConnection endpoint=$id threw=SecurityException")
            events.failed("Nearby permission is required. Check HOPPIE's app permissions and retry.")
        }
    }

    override fun reject(id: String, onComplete: (String?) -> Unit) {
        task(onComplete) { checkNotNull(client).rejectConnection(id).addOnSuccessListener { onComplete(null) } }
    }

    override fun stopSearch(role: NearbyRole) {
        if (searching != role) return
        searching = null
        when (role) {
            NearbyRole.Advertiser -> client?.stopAdvertising()
            NearbyRole.Discoverer -> client?.stopDiscovery()
        }
    }

    override fun close() {
        currentLink.value?.close()
        currentLink.value = null
        searching?.let(::stopSearch)
        client?.stopAllEndpoints()
        client = null
    }

    private inline fun task(noinline failed: (String) -> Unit, operation: () -> Task<Void>) {
        try {
            operation().addOnFailureListener { failed("Nearby connection error: ${it.localizedMessage}") }
        } catch (_: SecurityException) {
            failed("Nearby permission is required. Check HOPPIE's app permissions and retry.")
        }
    }
}
