package com.example.hoppie.features.receive.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.receive.DiscoveryStatus

@Composable
internal fun ReceiveMascot() {
    Box(
        Modifier.fillMaxWidth().padding(top = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(R.drawable.receive_mail),
            contentDescription = "Hoppie holding a telephone",
            modifier = Modifier.size(260.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
internal fun ReceiveDiscoveryStatus(status: DiscoveryStatus, modifier: Modifier = Modifier) {
    val (title, description) = when (status) {
        DiscoveryStatus.Starting, DiscoveryStatus.Scanning ->
            "Searching for devices..." to "Looking for senders nearby."
        DiscoveryStatus.Completed ->
            "Device search completed" to "Tap Rescan to search again."
        DiscoveryStatus.Cancelled ->
            "Connection cancelled" to "Tap Rescan to search for nearby devices."
        is DiscoveryStatus.Connecting -> {
            val peer = status.peerName.ifBlank { "nearby device" }
            "Connecting to $peer" to "Waiting for the sender to respond."
        }
        is DiscoveryStatus.Verifying -> {
            val peer = status.peerName.ifBlank { "nearby device" }
            "Confirm connection with $peer" to "Compare the code on both phones."
        }
        is DiscoveryStatus.AwaitingConfirmation -> {
            val peer = status.peerName.ifBlank { "nearby device" }
            "Connecting to $peer" to "Waiting for confirmation on the other phone."
        }
        DiscoveryStatus.Declining ->
            "Declining request" to "Closing the connection request."
        is DiscoveryStatus.Connected -> {
            val peer = status.peerName.ifBlank { "nearby device" }
            "Connected to $peer" to "Waiting for the sender to share files."
        }
        DiscoveryStatus.SessionActive ->
            "Nearby session active" to "End the current session to search again."
        is DiscoveryStatus.Unavailable ->
            "Device discovery unavailable" to status.message
    }

    Column(
        modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = HoppiePurple,
            textAlign = TextAlign.Center
        )
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF247C73),
            textAlign = TextAlign.Center
        )
    }
}
