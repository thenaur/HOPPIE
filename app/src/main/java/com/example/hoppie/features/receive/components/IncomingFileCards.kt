package com.example.hoppie.features.receive.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.components.FileSummaryCard
import com.example.hoppie.core.components.FileCategory
import com.example.hoppie.core.components.OfferedFile
import com.example.hoppie.core.theme.HoppieBlue
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.receive.IncomingFileOffer
import com.example.hoppie.features.send.formatFileSize

@Composable
internal fun SenderInfoCard(offer: IncomingFileOffer) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = HoppieBlue,
        contentColor = HoppiePurple
    ) {
        Row(
            modifier = Modifier.heightIn(min = 100.dp).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.profile_icon), null, Modifier.size(62.dp), contentScale = ContentScale.Fit)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(offer.senderName, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Sender", style = MaterialTheme.typography.bodyLarge)
                val count = offer.files.size
                val label = if (count == 1) "file" else "files"
                Text("$count $label • ${formatFileSize(offer.totalBytes)}", style = MaterialTheme.typography.bodyMedium)
            }
            Image(
                painterResource(R.drawable.stamp_icon),
                null,
                Modifier.size(30.dp).align(Alignment.Bottom),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
internal fun IncomingFileItem(file: OfferedFile, stacked: Boolean = false) {
    FileSummaryCard(file.name, FileCategory.from(file.name, file.mimeType), formatFileSize(file.sizeBytes), stacked)
}
