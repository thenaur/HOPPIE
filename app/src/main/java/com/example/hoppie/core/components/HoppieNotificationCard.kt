package com.example.hoppie.core.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HOPPIETheme
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple

internal sealed interface HoppieNotice {
    val message: String

    data class ConnectionDeclined(val deviceName: String) : HoppieNotice {
        val displayName: String get() = deviceName.ifBlank { "Nearby device" }
        override val message: String get() = "$displayName has declined request to connect"
    }

    data class FilesDeclined(val deviceName: String) : HoppieNotice {
        val displayName: String get() = deviceName.ifBlank { "Nearby device" }
        override val message: String get() = "$displayName has declined to receive the files"
    }

    data class ConnectionLost(
        val detail: String = "Your transfer was interrupted.\nOnly verified files are available."
    ) : HoppieNotice {
        override val message: String get() = "Connection Lost. $detail"
    }

    data class Info(val title: String, val detail: String? = null) : HoppieNotice {
        override val message: String get() = if (detail.isNullOrBlank()) title else "$title. $detail"
    }
}

@Composable
internal fun HoppieNotificationCard(
    notice: HoppieNotice,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tag = when (notice) {
        is HoppieNotice.ConnectionDeclined -> "hoppie_notice_connection_declined"
        is HoppieNotice.FilesDeclined -> "hoppie_notice_files_declined"
        is HoppieNotice.ConnectionLost -> "hoppie_notice_connection_lost"
        is HoppieNotice.Info -> "hoppie_notice_info"
    }
    Surface(
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .testTag(tag)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        contentColor = HoppiePurple,
        shadowElevation = 6.dp
    ) {
        Box(Modifier.fillMaxWidth()) {
            when (notice) {
                is HoppieNotice.FilesDeclined -> CenteredInfoNoticeContent(
                    icon = R.drawable.info_icon,
                    heading = notice.displayName,
                    supporting = "has declined to receive the files"
                )
                is HoppieNotice.ConnectionDeclined -> CenteredInfoNoticeContent(
                    icon = R.drawable.info_icon,
                    heading = notice.displayName,
                    supporting = "has declined request to connect"
                )
                is HoppieNotice.ConnectionLost -> ConnectionLostNoticeContent(
                    supporting = notice.detail
                )
                is HoppieNotice.Info -> HorizontalInfoNoticeContent(
                    heading = notice.title,
                    supporting = notice.detail
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp)
            ) {
                Icon(
                    painterResource(R.drawable.send_remove),
                    contentDescription = "Dismiss notification",
                    modifier = Modifier.size(13.dp),
                    tint = HoppieFileSize.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CenteredInfoNoticeContent(
    icon: Int,
    heading: String,
    supporting: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = heading,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = HoppiePurple,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodyLarge,
            color = HoppieFileSize,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ConnectionLostNoticeContent(supporting: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 38.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painterResource(R.drawable.wifi_error_icon),
            contentDescription = null,
            modifier = Modifier.size(54.dp),
            contentScale = ContentScale.Fit
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Connection Lost",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = HoppiePurple
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = HoppieFileSize
            )
        }
    }
}

@Composable
private fun HorizontalInfoNoticeContent(
    heading: String,
    supporting: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 38.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(
            painterResource(R.drawable.info_icon),
            contentDescription = null,
            modifier = Modifier.size(34.dp),
            contentScale = ContentScale.Fit
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = heading,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = HoppiePurple
            )
            if (!supporting.isNullOrBlank()) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HoppieFileSize
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun HoppieNotificationCardsPreview() {
    HOPPIETheme {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HoppieNotificationCard(HoppieNotice.FilesDeclined("Anna’s Phone"), onDismiss = {})
            HoppieNotificationCard(HoppieNotice.ConnectionDeclined("Haribon’s Phone"), onDismiss = {})
            HoppieNotificationCard(HoppieNotice.ConnectionLost(), onDismiss = {})
        }
    }
}
