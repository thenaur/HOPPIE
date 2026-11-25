package com.example.hoppie.features.receive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.components.FileTransferBackground
import com.example.hoppie.core.components.TransferHeader
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.receive.components.IncomingFileItem
import com.example.hoppie.features.receive.components.IncomingFilesActionsBar
import com.example.hoppie.features.receive.components.SenderInfoCard

@Composable
internal fun IncomingFilesScreen(
    state: IncomingFilesState,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    BackHandler { if (!state.busy) onDecline() }
    Surface(Modifier.fillMaxSize(), color = Color.White, contentColor = HoppiePurple) {
        Column {
            TransferHeader(onDecline, title = "Incoming Files", backEnabled = !state.busy)
            FileTransferBackground {
                Column(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "sender") { SenderInfoCard(state.offer) }
                        item(key = "heading") {
                            Text(
                                "Files to Receive",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 20.dp).semantics { heading() }
                            )
                        }
                        items(state.offer.files, key = { "file:${it.id}" }) { IncomingFileItem(it) }
                    }
                    IncomingFilesActionsBar(state, onAccept, onDecline)
                }
            }
        }
    }
}
