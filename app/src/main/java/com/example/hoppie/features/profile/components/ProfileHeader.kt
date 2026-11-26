package com.example.hoppie.features.profile.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.core.theme.HoppieWordmark

@Composable
internal fun ProfileHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.hoppie_head),
            contentDescription = null,
            modifier = Modifier.size(54.dp)
        )
        Text(
            text = "HOPPIE",
            color = HoppiePurple,
            style = HoppieWordmark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
