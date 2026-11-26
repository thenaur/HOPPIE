package com.example.hoppie.features.profile.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple

private val ProfileCardBorder = Color(0xFFE6ECF5)
private val ProfileCardDivider = Color(0xFFECF1F8)
private val ProfileCardShape = RoundedCornerShape(20.dp)

internal data class ProfileSettingItem(
    val title: String,
    val subtitle: String? = null,
    @field:DrawableRes val iconRes: Int,
    val iconSize: Dp = 24.dp,
    val onClick: () -> Unit
)

@Composable
internal fun ProfileSettingsCard(
    items: List<ProfileSettingItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = ProfileCardShape,
                ambientColor = Color(0x1A2E3A59),
                spotColor = Color(0x1A2E3A59)
            )
            .clip(ProfileCardShape)
            .background(Color.White, ProfileCardShape)
            .border(1.dp, ProfileCardBorder, ProfileCardShape)
    ) {
        items.forEachIndexed { index, item ->
            ProfileSettingRow(item = item)
            if (index < items.lastIndex) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = ProfileCardDivider
                )
            }
        }
    }
}

@Composable
private fun ProfileSettingRow(item: ProfileSettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (item.subtitle != null) 66.dp else 58.dp)
            .clickable(role = Role.Button, onClick = item.onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(item.iconSize),
                contentScale = ContentScale.Fit
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.title,
                color = HoppiePurple,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    color = HoppieFileSize,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Image(
            painter = painterResource(R.drawable.ic_profile_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}
