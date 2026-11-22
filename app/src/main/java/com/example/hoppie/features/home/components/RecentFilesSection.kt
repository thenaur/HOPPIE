package com.example.hoppie.features.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.core.theme.HoppieLinkBlue
import com.example.hoppie.features.home.RecentFile

@Composable
internal fun RecentFilesSection(files: List<RecentFile>, onSeeAll: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Recent files",
                modifier = Modifier.weight(1f).semantics { heading() },
                color = HoppiePurple,
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = onSeeAll, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("See all", color = HoppieLinkBlue, style = MaterialTheme.typography.labelLarge)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            files.forEach { RecentFileCard(it) }
        }
    }
}
