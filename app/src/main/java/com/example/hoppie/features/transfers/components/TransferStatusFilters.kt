package com.example.hoppie.features.transfers.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.transfers.TransferStatusFilter

@Composable
internal fun TransferStatusFilters(
    selected: TransferStatusFilter,
    onSelect: (TransferStatusFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransferStatusFilter.entries.forEach { filter ->
            val active = filter == selected
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) HoppiePurple else Color(0xFFE8F1FD))
                    .selectable(active, role = Role.Tab, onClick = { onSelect(filter) })
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    filter.label,
                    color = if (active) Color.White else HoppiePurple,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, lineHeight = 18.sp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
