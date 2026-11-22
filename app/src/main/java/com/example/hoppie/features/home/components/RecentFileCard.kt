package com.example.hoppie.features.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.components.FileCategory
import com.example.hoppie.core.theme.HoppieCardShadow
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.home.RecentFile

@Composable
internal fun RecentFileCard(file: RecentFile) {
    val shape = RoundedCornerShape(14.dp)
    Box(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Box(Modifier.matchParentSize().offset(y = 2.dp).background(HoppieCardShadow, shape))
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 53.dp)
                .background(Color.White, shape)
                .border(1.dp, HoppieCardShadow, shape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painterResource(FileCategory.from(file.name).icon),
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Fit
            )
            Column(Modifier.weight(1f)) {
                Text(
                    file.name,
                    color = HoppiePurple,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(file.size, color = HoppieFileSize, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
