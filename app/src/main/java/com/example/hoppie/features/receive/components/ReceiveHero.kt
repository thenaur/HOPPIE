package com.example.hoppie.features.receive.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.receive.DiscoveryStatus

@Composable
internal fun ReceiveHeader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Receive Files",
            modifier = Modifier.padding(horizontal = 52.dp, vertical = 6.dp).semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = HoppiePurple,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(48.dp)) {
            Icon(
                painterResource(R.drawable.send_back_arrow),
                contentDescription = "Back",
                modifier = Modifier.size(26.dp),
                tint = HoppiePurple
            )
        }
    }
}

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
