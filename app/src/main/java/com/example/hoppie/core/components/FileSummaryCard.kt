package com.example.hoppie.core.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple

@Composable
internal fun FileSummaryCard(name: String, category: FileCategory, size: String, stacked: Boolean = false) {
    Surface(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White,
        contentColor = HoppiePurple, shadowElevation = 3.dp
    ) {
        Row(
            Modifier.heightIn(min = 54.dp).padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(category.icon), null, Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (stacked) FileSizeLabel(size)
            }
            if (!stacked) FileSizeLabel(size)
        }
    }
}

@Composable
private fun FileSizeLabel(size: String) {
    Text(size, Modifier.widthIn(max = 92.dp), style = MaterialTheme.typography.bodyMedium, color = HoppieFileSize)
}
