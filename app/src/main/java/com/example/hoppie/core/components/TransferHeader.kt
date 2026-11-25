package com.example.hoppie.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppiePurple

@Composable
internal fun TransferHeader(
    onBack: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true
) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 52.dp, vertical = 6.dp).semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = HoppiePurple,
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = onBack,
            enabled = backEnabled,
            modifier = Modifier.align(Alignment.CenterStart).size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.send_back_arrow),
                contentDescription = "Back",
                modifier = Modifier.size(26.dp),
                tint = HoppiePurple
            )
        }
    }
}
