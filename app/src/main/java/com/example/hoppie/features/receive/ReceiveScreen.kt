package com.example.hoppie.features.receive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.components.FileTransferBackground
import com.example.hoppie.core.theme.HOPPIETheme
import com.example.hoppie.features.receive.components.ReceiveDiscoveryStatus
import com.example.hoppie.features.receive.components.ReceiveHeader
import com.example.hoppie.features.receive.components.ReceiveMascot
import com.example.hoppie.features.receive.components.RescanButton
import com.example.hoppie.features.receive.components.nearbyDevicesSection

@Composable
internal fun ReceiveScreen(onBack: () -> Unit) {
    ReceiveContent(
        state = ReceiveUiState(
            discovery = DiscoveryStatus.Unavailable("Nearby device discovery is not available in this version.")
        ),
        onBack = onBack
    )
}

@Composable
internal fun ReceiveContent(
    state: ReceiveUiState,
    onBack: () -> Unit,
    onRescan: (() -> Unit)? = null,
    onConnect: ((String) -> Unit)? = null
) {
    FileTransferBackground {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
            ReceiveHeader(onBack, Modifier.padding(top = 8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "mascot") { ReceiveMascot() }
                item(key = "discovery") {
                    ReceiveDiscoveryStatus(state.discovery, Modifier.padding(top = 8.dp, bottom = 12.dp))
                }
                nearbyDevicesSection(state.devices, onConnect)
            }
            RescanButton(
                onRescan = onRescan,
                enabled = state.discovery != DiscoveryStatus.Starting,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 820)
@Composable
private fun ReceiveDevicesPreview() {
    HOPPIETheme {
        ReceiveContent(
            state = ReceiveUiState(
                discovery = DiscoveryStatus.Scanning,
                devices = listOf(
                    NearbyDevice("preview-1", "Example Phone", DeviceConnection.Connecting),
                    NearbyDevice("preview-2", "Example Tablet"),
                    NearbyDevice("preview-3", "Example Laptop", DeviceConnection.Connected),
                    NearbyDevice("preview-4", "Example Device", DeviceConnection.Failed("Could not connect. Try again."))
                )
            ),
            onBack = {},
            onRescan = {},
            onConnect = {}
        )
    }
}
