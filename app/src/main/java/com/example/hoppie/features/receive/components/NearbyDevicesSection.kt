package com.example.hoppie.features.receive.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppieBlue
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.receive.DeviceConnection
import com.example.hoppie.features.receive.NearbyDevice

internal fun LazyListScope.nearbyDevicesSection(devices: List<NearbyDevice>, onConnect: ((String) -> Unit)?) {
    item(key = "found-nearby") {
        Text(
            "Found Nearby (${devices.size})",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = HoppiePurple
        )
    }
    items(devices, key = { "device:${it.id}" }) { device ->
        NearbyDeviceItem(device, onConnect)
    }
}

@Composable
private fun NearbyDeviceItem(device: NearbyDevice, onConnect: ((String) -> Unit)?) {
    val label = when (device.connection) {
        DeviceConnection.Available -> "Connect"
        DeviceConnection.Connecting -> "Connecting..."
        DeviceConnection.Connected -> "Connected"
        is DeviceConnection.Failed -> "Retry"
    }
    val canConnect = device.connection == DeviceConnection.Available || device.connection is DeviceConnection.Failed

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HoppieBlue)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painterResource(R.drawable.phone_icon),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    device.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = HoppiePurple,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Button(
                    onClick = { onConnect?.invoke(device.id) },
                    enabled = canConnect && onConnect != null,
                    modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(0.42f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HoppiePurple,
                        contentColor = Color.White,
                        disabledContainerColor = HoppiePurple.copy(alpha = 0.12f),
                        disabledContentColor = HoppiePurple
                    )
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
            val failure = device.connection as? DeviceConnection.Failed
            if (failure != null) {
                Text(
                    failure.message,
                    modifier = Modifier.padding(start = 42.dp, top = 4.dp, bottom = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium,
                    color = HoppieFileSize
                )
            }
        }
    }
}
