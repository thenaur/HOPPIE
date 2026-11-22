package com.example.hoppie.features.home.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.hoppie.R

@Composable
internal fun HomeMascot(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.hoppie_home),
        contentDescription = "Hoppie, the mail-carrier bunny",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
