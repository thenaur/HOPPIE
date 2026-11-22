package com.example.hoppie.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.example.hoppie.R

private val RustyHooks = FontFamily(Font(R.font.rusty_hooks))
private val Hagoya = FontFamily(Font(R.font.hagoya))
private val FriendlySans = FontFamily(Font(R.font.friendly_sans))

val HoppieWordmark = TextStyle(fontFamily = RustyHooks, fontSize = 28.sp, lineHeight = 34.sp)

val Typography = Typography(
    headlineSmall = TextStyle(fontFamily = Hagoya, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Hagoya, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Hagoya, fontSize = 19.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontFamily = Hagoya, fontSize = 16.sp, lineHeight = 21.sp),
    labelSmall = TextStyle(fontFamily = Hagoya, fontSize = 11.sp, lineHeight = 15.sp),
    bodyLarge = TextStyle(fontFamily = FriendlySans, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FriendlySans, fontSize = 14.sp, lineHeight = 19.sp)
)
