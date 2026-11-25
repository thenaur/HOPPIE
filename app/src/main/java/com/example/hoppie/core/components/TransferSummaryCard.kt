package com.example.hoppie.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple

@Composable
internal fun TransferSummaryCard(fileCount: Int, formattedTotalSize: String, receiving: Boolean, peerName: String?) {
    val direction = if (receiving) "received" else "sent"
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFEBF2FC), contentColor = HoppiePurple) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryStatistic(fileCount.toString(), "Files $direction", Modifier.weight(1f))
                SummaryStatistic(formattedTotalSize, "Total $direction", Modifier.weight(1f))
            }
            if (!peerName.isNullOrBlank()) {
                HorizontalDivider(color = HoppiePurple.copy(alpha = 0.12f))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (receiving) "Sender" else "Recipient", style = MaterialTheme.typography.labelMedium, color = HoppieFileSize)
                    Text(peerName, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SummaryStatistic(value: String, label: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = HoppieFileSize)
    }
}
