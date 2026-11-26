package com.example.hoppie.features.profile.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple

private val EditButtonFill = Color(0xFFF3F7FD)
private val EditButtonBorder = Color(0xFFDDE6F4)

@Composable
internal fun ProfileIdentitySection(
    deviceName: String,
    deviceSubtitle: String,
    onEditDeviceName: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.hoppie_profile),
            contentDescription = "Hoppie device avatar",
            modifier = Modifier.size(132.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = deviceName,
                    color = HoppiePurple,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = deviceSubtitle,
                    color = HoppieFileSize,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EditButtonFill, CircleShape)
                    .border(1.dp, EditButtonBorder, CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Edit device name",
                        onClick = onEditDeviceName
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_profile_edit),
                    contentDescription = "Edit device name",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
