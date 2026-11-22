package com.example.hoppie.features.receive.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppieBlue
import com.example.hoppie.core.theme.HoppieLinkBlue
import com.example.hoppie.core.theme.HoppiePurple

@Composable
internal fun RescanButton(onRescan: (() -> Unit)?, enabled: Boolean, modifier: Modifier = Modifier) {
    Button(
        onClick = { onRescan?.invoke() },
        enabled = enabled && onRescan != null,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HoppieBlue.copy(alpha = 0.38f),
            contentColor = HoppiePurple,
            disabledContainerColor = HoppieBlue.copy(alpha = 0.24f),
            disabledContentColor = HoppiePurple.copy(alpha = 0.68f)
        )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_refresh),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = HoppieLinkBlue
        )
        Spacer(Modifier.width(8.dp))
        Text("Rescan", style = MaterialTheme.typography.titleLarge)
    }
}
