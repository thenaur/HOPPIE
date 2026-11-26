package com.example.hoppie.features.library.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.core.utils.StorageFormatter
import com.example.hoppie.data.local.entities.ReceivedFileEntity

private val CardShape = RoundedCornerShape(18.dp)
private val CardBorder = Color(0xFFE6ECF5)
private val DeleteRed = Color(0xFFC93B3B)

private data class FileBadgeStyle(
    val background: Color,
    @field:DrawableRes val iconRes: Int,
    val isPdfBadge: Boolean = false
)

private fun fileBadgeStyle(file: ReceivedFileEntity): FileBadgeStyle {
    return when {
        StorageFormatter.isPdf(file.fileName) -> FileBadgeStyle(
            background = Color(0xFFFDECEC),
            iconRes = R.drawable.ic_library_file_pdf,
            isPdfBadge = true
        )

        file.category == StorageFormatter.CATEGORY_IMAGES || StorageFormatter.isImage(file.fileName) -> FileBadgeStyle(
            background = Color(0xFFE7F2FE),
            iconRes = R.drawable.ic_library_file_image
        )

        file.category == StorageFormatter.CATEGORY_CODE -> FileBadgeStyle(
            background = Color(0xFFEEEBFC),
            iconRes = R.drawable.ic_library_file_code
        )

        else -> FileBadgeStyle(
            background = Color(0xFFFFF1E4),
            iconRes = R.drawable.ic_library_file_doc
        )
    }
}

@Composable
internal fun LibraryFileListCard(
    file: ReceivedFileEntity,
    onPreview: () -> Unit,
    onRedistribute: () -> Unit,
    onShowInfo: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = CardShape,
                ambientColor = Color(0x182E3A59),
                spotColor = Color(0x182E3A59)
            )
            .clip(CardShape)
            .background(Color.White, CardShape)
            .border(1.dp, CardBorder, CardShape)
            .clickable(role = Role.Button, onClick = onPreview)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LibraryFileBadge(file = file)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = file.fileName,
                color = HoppiePurple,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${StorageFormatter.formatBytes(file.fileSizeBytes)} · from ${file.senderName}",
                color = HoppieFileSize,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FileOverflowMenuButton(
            expanded = menuExpanded,
            onExpand = { menuExpanded = true },
            onDismiss = { menuExpanded = false },
            onPreview = onPreview,
            onRedistribute = onRedistribute,
            onShowInfo = onShowInfo,
            onDelete = onDelete
        )
    }
}

@Composable
internal fun LibraryFileGridCard(
    file: ReceivedFileEntity,
    onPreview: () -> Unit,
    onRedistribute: () -> Unit,
    onShowInfo: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = CardShape,
                ambientColor = Color(0x182E3A59),
                spotColor = Color(0x182E3A59)
            )
            .clip(CardShape)
            .background(Color.White, CardShape)
            .border(1.dp, CardBorder, CardShape)
            .clickable(role = Role.Button, onClick = onPreview)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            LibraryFileBadge(file = file)
            FileOverflowMenuButton(
                expanded = menuExpanded,
                onExpand = { menuExpanded = true },
                onDismiss = { menuExpanded = false },
                onPreview = onPreview,
                onRedistribute = onRedistribute,
                onShowInfo = onShowInfo,
                onDelete = onDelete
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = file.fileName,
            color = HoppiePurple,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "${StorageFormatter.formatBytes(file.fileSizeBytes)} · ${file.senderName}",
            color = HoppieFileSize,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LibraryFileBadge(file: ReceivedFileEntity) {
    val style = fileBadgeStyle(file)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(style.background, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(style.iconRes),
            contentDescription = null,
            modifier = Modifier.size(26.dp)
        )
        if (style.isPdfBadge) {
            Text(
                text = "PDF",
                color = Color.White,
                fontSize = 6.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun FileOverflowMenuButton(
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onPreview: () -> Unit,
    onRedistribute: () -> Unit,
    onShowInfo: () -> Unit,
    onDelete: () -> Unit
) {
    Box {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClickLabel = "File options", onClick = onExpand),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_library_more_vert),
                contentDescription = "File options",
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            containerColor = Color.White
        ) {
            DropdownMenuItem(
                text = { Text("Preview", color = HoppiePurple, style = MaterialTheme.typography.bodyMedium) },
                onClick = {
                    onDismiss()
                    onPreview()
                }
            )
            DropdownMenuItem(
                text = { Text("Share / Redistribute", color = HoppiePurple, style = MaterialTheme.typography.bodyMedium) },
                onClick = {
                    onDismiss()
                    onRedistribute()
                }
            )
            DropdownMenuItem(
                text = { Text("File Info", color = HoppiePurple, style = MaterialTheme.typography.bodyMedium) },
                onClick = {
                    onDismiss()
                    onShowInfo()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = DeleteRed, style = MaterialTheme.typography.bodyMedium) },
                onClick = {
                    onDismiss()
                    onDelete()
                }
            )
        }
    }
}
