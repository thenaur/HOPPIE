package com.example.hoppie.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HoppieColors = darkColorScheme(
    primary = HoppieLavender,
    onPrimary = HoppieBlack,
    secondary = HoppieBlue,
    onSecondary = HoppiePurple,
    background = Color.White,
    onBackground = HoppieBlack,
    surface = HoppieBlack,
    onSurface = HoppieWhite
)

@Composable
fun HOPPIETheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = HoppieColors, typography = Typography, content = content)
}
