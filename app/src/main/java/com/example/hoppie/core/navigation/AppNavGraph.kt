package com.example.hoppie.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hoppie.core.components.BottomNavBar
import com.example.hoppie.features.home.HomeScreen
import com.example.hoppie.features.library.LibraryScreen
import com.example.hoppie.features.profile.ProfileScreen
import com.example.hoppie.features.receive.ReceiveScreen
import com.example.hoppie.features.send.SendScreen
import com.example.hoppie.features.transfers.TransfersScreen

@Composable
fun AppNavGraph(onSendVisibilityChanged: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Screen.Home.route
    val isSend = currentRoute == Screen.Send.route
    val selected = MainDestination.entries.firstOrNull { it.screen.route == currentRoute }

    LaunchedEffect(isSend) { onSendVisibilityChanged(isSend) }

    fun selectMain(destination: MainDestination) {
        if (destination.screen.route == navController.currentDestination?.route) return
        navController.navigate(destination.screen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val colors = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.background,
        contentColor = colors.onBackground,
        bottomBar = {
            if (selected != null) BottomNavBar(selected, ::selectMain)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize().padding(padding)
                .background(colors.background).consumeWindowInsets(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onSend = {
                        navController.navigate(Screen.Send.route) { launchSingleTop = true }
                    },
                    onReceive = {
                        navController.navigate(Screen.Receive.route) { launchSingleTop = true }
                    },
                    onSeeAll = { selectMain(MainDestination.Library) }
                )
            }
            composable(Screen.Library.route) { LibraryScreen() }
            composable(Screen.Transfers.route) { TransfersScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.Send.route) { SendScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Receive.route) { ReceiveScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
