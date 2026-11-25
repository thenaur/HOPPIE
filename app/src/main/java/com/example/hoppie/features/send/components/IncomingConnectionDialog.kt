package com.example.hoppie.features.send.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.data.nearby.SessionState
import com.example.hoppie.features.send.IncomingConnectionRequest
import com.example.hoppie.features.send.IncomingConnectionStatus
import com.example.hoppie.features.send.IncomingConnectionUiState

@Composable
internal fun NearbyConnectionDialog(prompt: SessionState, onDecision: (SessionState, Boolean) -> Unit) {
    val peer = checkNotNull(prompt.peer)
    IncomingConnectionDialog(
        state = IncomingConnectionUiState(IncomingConnectionRequest(peer.id, peer.name)),
        onAccept = { onDecision(prompt, true) },
        onDecline = { onDecision(prompt, false) },
        authenticationDigits = prompt.digits
    )
}

@Composable
internal fun IncomingConnectionDialog(
    state: IncomingConnectionUiState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    authenticationDigits: String? = null
) {
    val awaitingDecision = state.status == IncomingConnectionStatus.AwaitingDecision
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            contentColor = HoppiePurple,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    Image(
                        painterResource(R.drawable.phone_icon),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                    Column(Modifier.weight(1f, fill = false)) {
                        val name = state.request.deviceName.ifBlank { "Nearby device" }
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() },
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "wants to connect",
                            style = MaterialTheme.typography.bodyLarge,
                            color = HoppieFileSize
                        )
                    }
                }
                authenticationDigits?.let { digits ->
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFEAF3FF), contentColor = HoppiePurple) {
                        Column(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Compare this code on both phones", textAlign = TextAlign.Center)
                            Text(digits, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
                state.error?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = HoppieFileSize,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ConnectionDialogButton(
                        label = if (state.status == IncomingConnectionStatus.Accepting) {
                            "Connecting…"
                        } else "Accept",
                        color = HoppiePurple,
                        enabled = awaitingDecision,
                        busy = state.status == IncomingConnectionStatus.Accepting,
                        onClick = onAccept
                    )
                    ConnectionDialogButton(
                        label = if (state.status == IncomingConnectionStatus.Declining) {
                            "Declining…"
                        } else {
                            "Decline Request"
                        },
                        color = Color(0xFF247C73),
                        enabled = awaitingDecision,
                        busy = state.status == IncomingConnectionStatus.Declining,
                        onClick = onDecline
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionDialogButton(
    label: String,
    color: Color,
    enabled: Boolean,
    busy: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = color.copy(alpha = 0.14f),
            disabledContentColor = color
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = color,
                    strokeWidth = 2.dp
                )
            }
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}
