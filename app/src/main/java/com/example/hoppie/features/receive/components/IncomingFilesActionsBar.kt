package com.example.hoppie.features.receive.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.receive.IncomingFilesState

@Composable
internal fun IncomingFilesActionsBar(state: IncomingFilesState, onAccept: () -> Unit, onDecline: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        state.error?.let {
            Text(it, color = HoppieFileSize, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        IncomingAction(
            if (state.busy && state.accepting) "Preparing…" else "Receive Files",
            Color(0xFFE8F1FD), HoppiePurple, !state.busy, onAccept, downloadIcon = true
        )
        IncomingAction(
            if (state.busy && !state.accepting) "Declining…" else "Decline Files",
            HoppiePurple, Color.White, !state.busy, onDecline
        )
    }
}

@Composable
internal fun IncomingAction(
    label: String,
    background: Color,
    foreground: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    downloadIcon: Boolean = false,
    supportingLabel: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(shape = shape, color = background, shadowElevation = 4.dp) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = foreground)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (downloadIcon) DownloadIcon()
                leadingIcon?.invoke()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    supportingLabel?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadIcon() {
    Canvas(Modifier.size(24.dp)) {
        val teal = Color(0xFF228C83)
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val glyph = Path().apply {
            moveTo(size.width * 0.25f, size.height * 0.46f)
            lineTo(size.width * 0.5f, size.height * 0.69f)
            lineTo(size.width * 0.75f, size.height * 0.46f)
            moveTo(size.width * 0.12f, size.height * 0.74f)
            lineTo(size.width * 0.12f, size.height * 0.92f)
            lineTo(size.width * 0.88f, size.height * 0.92f)
            lineTo(size.width * 0.88f, size.height * 0.74f)
        }
        drawPath(glyph, teal, style = stroke)
        drawLine(teal, Offset(size.width * 0.5f, size.height * 0.08f),
            Offset(size.width * 0.5f, size.height * 0.69f), stroke.width, StrokeCap.Round)
    }
}
