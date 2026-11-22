package com.example.hoppie.core.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.navigation.MainDestination
import com.example.hoppie.core.theme.HoppieBlue
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.core.theme.HoppieWhite

@Composable
internal fun BottomNavBar(selected: MainDestination, onSelect: (MainDestination) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .background(HoppieBlue, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().selectableGroup()
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            MainDestination.entries.forEach { destination ->
                val isSelected = selected == destination
                Column(
                    modifier = Modifier.weight(1f).heightIn(min = 60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelect(destination) }
                        )
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(width = 54.dp, height = 42.dp)
                            .background(
                                if (isSelected) HoppieWhite.copy(alpha = 0.60f) else HoppieBlue,
                                RoundedCornerShape(15.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painterResource(destination.icon),
                            contentDescription = null,
                            modifier = Modifier.size(destination.iconSize)
                        )
                    }
                    Text(
                        destination.label,
                        color = HoppiePurple,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
