package com.example.hoppie.features.send.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.hoppie.core.theme.HoppiePurple

@Composable
internal fun SendButton(enabled: Boolean, onSend: () -> Unit) {
    Button(
        onClick = onSend,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HoppiePurple,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF281C4C),
            disabledContentColor = Color(0xFFC6BADF)
        )
    ) {
        Text("Send", style = MaterialTheme.typography.titleLarge)
    }
}
