package com.example.hoppie.features.home.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppieCardShadow
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.core.theme.HoppieWhite

@Composable
internal fun SendReceiveButtons(onSend: () -> Unit, onReceive: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FileActionButton("Send Files", R.drawable.send_icon, HoppiePurple, HoppieWhite, onSend)
        FileActionButton("Receive Files", R.drawable.receive_icon, HoppieWhite, HoppiePurple, onReceive)
    }
}

@Composable
private fun FileActionButton(
    label: String,
    @DrawableRes icon: Int,
    background: Color,
    foreground: Color,
    onClick: () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().offset(y = 2.dp).background(HoppieCardShadow, CircleShape))
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = foreground)
        ) {
            Image(painterResource(icon), null, Modifier.size(34.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.titleLarge)
        }
    }
}
