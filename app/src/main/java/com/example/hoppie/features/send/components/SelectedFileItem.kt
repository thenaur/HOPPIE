package com.example.hoppie.features.send.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppieCardShadow
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.send.SelectedFile
import com.example.hoppie.features.send.formatFileSize

@Composable
internal fun SelectedFileItem(file: SelectedFile, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().offset(y = 2.dp).background(HoppieCardShadow, shape))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).background(Color.White, shape)
                .border(1.dp, HoppieCardShadow, shape)
                .padding(start = 14.dp, end = 5.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painterResource(file.category.icon),
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Fit
            )
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = HoppiePurple,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatFileSize(file.sizeBytes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HoppieFileSize
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                Icon(
                    painterResource(R.drawable.send_remove),
                    contentDescription = "Remove ${file.name}",
                    modifier = Modifier.size(22.dp),
                    tint = Color(0xFF3A9C91)
                )
            }
        }
    }
}
