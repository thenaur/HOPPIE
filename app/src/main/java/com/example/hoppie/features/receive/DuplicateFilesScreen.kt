package com.example.hoppie.features.receive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.components.FileTransferBackground
import com.example.hoppie.core.components.OfferedFile
import com.example.hoppie.core.components.TransferHeader
import com.example.hoppie.core.theme.HoppieLinkBlue
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.receive.components.IncomingAction
import com.example.hoppie.features.receive.components.IncomingFileItem

@Composable
internal fun DuplicateFilesScreen(
    file: OfferedFile,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onCopy: () -> Unit
) {
    BackHandler { if (!busy) onBack() }
    Surface(Modifier.fillMaxSize(), color = Color.White, contentColor = HoppiePurple) {
        Column {
            TransferHeader(onBack, title = "Duplicate Files", backEnabled = !busy)
            FileTransferBackground {
                Column(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painterResource(R.drawable.duplicate_mail),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(0.82f).heightIn(max = 272.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            "The file already exists on your device",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "“${file.name}” is duplicate.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF247C73)
                        )
                        IncomingFileItem(file, stacked = true)
                    }
                    DuplicateActions(busy, error, onSkip, onCopy)
                }
            }
        }
    }
}

@Composable
private fun DuplicateActions(busy: Boolean, error: String?, onSkip: () -> Unit, onCopy: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        error?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (busy) Text("Preparing transfer…", style = MaterialTheme.typography.bodyMedium)
        IncomingAction("Skip Transfer", Color(0xFFE8F1FD), HoppiePurple, !busy, onSkip, leadingIcon = {
            Icon(painterResource(R.drawable.send_remove), null, Modifier.size(22.dp), tint = HoppieLinkBlue)
        })
        IncomingAction(
            "Receive Anyway", HoppiePurple, Color.White, !busy, onCopy,
            supportingLabel = "(Create another copy)"
        )
    }
}
