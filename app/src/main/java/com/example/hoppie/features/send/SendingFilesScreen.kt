package com.example.hoppie.features.send

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.components.FileSummaryCard
import com.example.hoppie.core.components.FileTransferBackground
import com.example.hoppie.core.components.TransferHeader
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.data.nearby.Phase
import com.example.hoppie.data.nearby.SessionState

@Composable
internal fun SendingFilesScreen(state: SendUiState, session: SessionState, onStop: () -> Unit) {
    FileTransferBackground {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().padding(vertical = 8.dp)) {
            TransferHeader(onStop, title = "Sending Files")
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    SharingStatus(session, state.connectionNotice?.message)
                }
                item {
                    Text(
                        "Sharing files (${state.files.size})",
                        Modifier.fillMaxWidth().padding(top = 8.dp).semantics { heading() },
                        style = MaterialTheme.typography.titleMedium, color = HoppiePurple
                    )
                }
                items(state.files, key = { it.uri.toString() }) { file ->
                    FileSummaryCard(file.name, file.category, formatFileSize(file.sizeBytes))
                }
            }
            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp).heightIn(min = 54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF3FF), contentColor = HoppiePurple)
            ) {
                Icon(painterResource(R.drawable.send_remove), null, Modifier.size(22.dp), tint = Color(0xFF5079DA))
                Spacer(Modifier.width(8.dp))
                Text("Stop Sharing", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun SharingStatus(session: SessionState, message: String?) {
    val peer = session.peer?.name?.ifBlank { "nearby device" } ?: "nearby device"
    val heading = when (session.phase) {
        Phase.Starting -> "Starting nearby sharing"
        Phase.AwaitingConnectionResult -> "Connecting to $peer"
        Phase.Declining -> "Declining request"
        Phase.Connected -> if (message == null) "Connected to $peer" else "File offer ended"
        else -> "Waiting for a nearby device to connect"
    }
    val instruction = when (session.phase) {
        Phase.AwaitingConnectionResult -> "Waiting for confirmation on the other phone"
        Phase.Connected -> message ?: "Waiting for the receiver to review the files."
        Phase.Declining -> "Closing the connection request"
        else -> "Ask the receiver to tap Connect in HOPPIE."
    }
    Column(
        Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(painterResource(R.drawable.send_mail), null, Modifier.size(210.dp))
        Column(
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(heading, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, color = HoppiePurple)
            Text(instruction, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF247C73))
        }
    }
}
