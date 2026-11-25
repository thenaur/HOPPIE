package com.example.hoppie.features.send

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.components.FileTransferBackground
import com.example.hoppie.core.components.TransferCompletionScreen
import com.example.hoppie.core.components.TransferHeader
import com.example.hoppie.core.theme.HoppieLinkBlue
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.send.components.OverallTransferProgress
import com.example.hoppie.features.send.components.TransferFileItem

@Composable
internal fun TransferringFilesScreen(
    state: FileTransferState,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    exitLabel: String = "Back to Send Files",
    onOpenTransfers: (() -> Unit)? = null
) {
    if (state.status == TransferStatus.Completed) {
        TransferCompletionScreen(state, state.recipientName, onDone)
        return
    }
    if (state.status == TransferStatus.Failed) {
        TransferFailedScreen(state, onExit = onDone)
        return
    }
    BackHandler { if (!state.active) onDone() }
    val label = when {
        state.cancelling -> "Cancelling…"
        state.active -> "Cancel Transfer"
        else -> exitLabel
    }
    TransferScreenLayout(
        mascot = if (state.status == TransferStatus.Cancelled) R.drawable.failed_mail else R.drawable.transfer_mail,
        onBack = onDone,
        backEnabled = !state.active,
        footer = {
            if (state.active && onOpenTransfers != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TransferActionButton(
                        label = "Transfers",
                        onClick = onOpenTransfers,
                        enabled = !state.cancelling,
                        modifier = Modifier.fillMaxWidth(),
                        icon = R.drawable.transfers_icon,
                        primary = true
                    )
                    TransferActionButton(
                        label = label,
                        enabled = !state.cancelling,
                        modifier = Modifier.fillMaxWidth(),
                        icon = R.drawable.send_remove,
                        onClick = onCancel
                    )
                }
            } else {
                TransferActionButton(
                    label = label,
                    enabled = !state.cancelling,
                    icon = if (state.active) R.drawable.send_remove else null,
                    onClick = if (state.active) onCancel else onDone
                )
            }
        }
    ) {
        item(key = "overall") {
            OverallTransferProgress(state, Modifier.fillMaxWidth(0.88f))
        }
        item(key = "file-heading") { TransferFilesHeading(state.files.size) }
        items(state.files, key = { "transfer:${it.file.uri}" }) {
            TransferFileItem(it)
        }
    }
}

@Composable
internal fun TransferScreenLayout(
    @DrawableRes mascot: Int,
    onBack: () -> Unit,
    backEnabled: Boolean,
    footer: @Composable () -> Unit,
    content: LazyListScope.() -> Unit
) {
    Surface(Modifier.fillMaxSize(), color = Color.White, contentColor = HoppiePurple) {
        Column {
            TransferHeader(onBack, title = "Transferring Files", backEnabled = backEnabled)
            FileTransferBackground {
                Column(
                    Modifier.widthIn(max = 560.dp).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "mascot") {
                            Image(
                                painterResource(mascot),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .heightIn(max = 196.dp)
                                    .testTag(
                                        if (mascot == R.drawable.failed_mail) "transfer_illustration_failed_mail"
                                        else "transfer_illustration_active"
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        }
                        content()
                    }
                    footer()
                }
            }
        }
    }
}

@Composable
internal fun TransferFilesHeading(count: Int) {
    Text(
        "Files ($count)",
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).semantics { heading() },
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
internal fun TransferActionButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
    @DrawableRes icon: Int? = null,
    primary: Boolean = false
) {
    val shape = RoundedCornerShape(14.dp)
    val background = if (primary) HoppiePurple else Color(0xFFE8F1FD)
    val foreground = if (primary) Color.White else HoppiePurple
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = background,
                contentColor = foreground,
                disabledContainerColor = if (primary) background.copy(alpha = 0.65f) else background,
                disabledContentColor = foreground
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    Icon(
                        painterResource(it), null, Modifier.size(22.dp),
                        tint = if (primary) Color(0xFF83A9FF) else HoppieLinkBlue
                    )
                }
                Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
        }
    }
}
