package com.example.hoppie.features.send.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.theme.HoppiePurple
import java.text.NumberFormat

@Composable
internal fun SelectedFilesSummary(count: Int, totalBytes: Long) {
    val fileLabel = if (count == 1) "file" else "files"
    val totalMegabytes = totalBytes / (1024.0 * 1024)
    val formattedTotal = NumberFormat.getNumberInstance().apply {
        maximumFractionDigits = 1
    }.format(totalMegabytes)

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFE5F0FF),
        contentColor = HoppiePurple
    ) {
        Text(
            "$count $fileLabel • $formattedTotal MB selected",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 15.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
