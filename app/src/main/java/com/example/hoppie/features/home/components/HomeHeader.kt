package com.example.hoppie.features.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
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
internal fun HomeHeader(onNotifications: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.hoppie_head), null, Modifier.size(54.dp))
            Text(
                "HOPPIE",
                color = HoppiePurple,
                style = HoppieWordmark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onNotifications, modifier = Modifier.size(48.dp)) {
            Image(painterResource(R.drawable.notification_bell), "Notifications", Modifier.size(36.dp))
        }
    }
}
