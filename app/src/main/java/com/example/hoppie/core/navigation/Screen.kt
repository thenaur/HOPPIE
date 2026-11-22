package com.example.hoppie.core.navigation

import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hoppie.R

internal enum class Screen(val route: String) {
    Home("home"),
    Library("library"),
    Transfers("transfers"),
    Profile("profile"),
    Send("send"),
    Receive("receive")
}

internal enum class MainDestination(
    val screen: Screen,
    val label: String,
    @field:DrawableRes val icon: Int,
    val iconSize: Dp
) {
    Home(Screen.Home, "Home", R.drawable.home_icon, 42.dp),
    Transfers(Screen.Transfers, "Transfer", R.drawable.transfers_icon, 32.dp),
    Library(Screen.Library, "Library", R.drawable.library_icon, 42.dp),
    Profile(Screen.Profile, "Profile", R.drawable.profile_icon, 32.dp)
}
