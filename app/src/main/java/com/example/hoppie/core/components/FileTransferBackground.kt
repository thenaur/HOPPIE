package com.example.hoppie.core.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.hoppie.R

@Composable
internal fun FileTransferBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.TopCenter) {
        Image(
            painter = painterResource(R.drawable.topbg_blue),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.FillWidth
        )
        content()
    }
}
