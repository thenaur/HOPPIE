package com.example.hoppie.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hoppie.HoppieApplication
import com.example.hoppie.core.components.BottomNavBar
import com.example.hoppie.data.nearby.NearbyRole
import com.example.hoppie.data.nearby.Phase
import com.example.hoppie.features.home.HomeScreen
import com.example.hoppie.features.library.LibraryScreen
import com.example.hoppie.features.profile.ProfileScreen
import com.example.hoppie.features.receive.ReceiveScreen
import com.example.hoppie.features.receive.ReceiveViewModel
import com.example.hoppie.features.send.SendScreen
import com.example.hoppie.features.send.SendViewModel
import com.example.hoppie.features.send.TransferStatus
import com.example.hoppie.features.send.isPayloadTransferActive
import com.example.hoppie.features.transfers.CompletedTransferHistory
import com.example.hoppie.features.transfers.TransfersScreen

@Composable
fun AppNavGraph(onSendVisibilityChanged: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as HoppieApplication
    val nearby = app.nearby
    val link by app.transport.link.collectAsStateWithLifecycle()
    val reopenCoordinator: AppReopenCoordinator = viewModel()
    val sender: SendViewModel = viewModel()
    val receiver: ReceiveViewModel = viewModel {
        ReceiveViewModel { transfer, peer -> CompletedTransferHistory(app).record(transfer, received = true, peerName = peer) }
    }
    var ongoingFocusKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(link) {
        val connection = link
        if (connection == null) {
            receiver.state.value.incoming?.let { receiver.onIncomingOfferEnded(it.offer.sessionId, "Disconnected.") }
        } else {
            connection.incoming.collect { offer ->
                if (offer != null) {
                    if (receiver.state.value.incoming?.offer?.sessionId != offer.sessionId &&
                        !receiver.onIncomingFileOffer(offer, connection)) {
                        connection.rejectBusy(offer)
                    }
                } else if (receiver.state.value.transfer == null) {
                    receiver.state.value.incoming?.let { receiver.onIncomingOfferEnded(it.offer.sessionId, "The file offer ended.") }
                }
            }
        }
    }
    val session by nearby.state.collectAsStateWithLifecycle()
    val sendingState by sender.state.collectAsStateWithLifecycle()
    val receivingState by receiver.state.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Screen.Home.route
    val isSend = currentRoute == Screen.Send.route
    val senderSession = session.takeIf { isSend && it.role == NearbyRole.Advertiser }
    val selected = MainDestination.entries.firstOrNull { it.screen.route == currentRoute }

    LaunchedEffect(isSend) { onSendVisibilityChanged(isSend) }

    LaunchedEffect(
        sendingState.transfer?.status,
        sendingState.transfer?.files?.size,
        receivingState.transfer?.status,
        receivingState.transfer?.files?.size
    ) {
        reopenCoordinator.syncTransferState(sendingState.transfer, receivingState.transfer)
    }

    LaunchedEffect(session.phase) {
        if (session.phase == Phase.Idle) sender.stopWaitingForReceiver()
    }

    fun selectMain(destination: MainDestination, savePoppedState: Boolean = true) {
        if (destination.screen.route == navController.currentDestination?.route) return
        navController.navigate(destination.screen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = savePoppedState }
            launchSingleTop = true
            restoreState = savePoppedState
        }
    }

    fun returnHome() {
        if (navController.currentDestination?.route == Screen.Home.route) return
        if (!navController.popBackStack(Screen.Home.route, inclusive = false)) {
            selectMain(MainDestination.Home, savePoppedState = false)
        }
    }

    fun openTransfersFromProgress() {
        ongoingFocusKey++
        selectMain(MainDestination.Transfers, savePoppedState = false)
    }

    val readyForNavigation = backStackEntry != null
    val pendingAction = reopenCoordinator.pendingAction
    val actionSerial = reopenCoordinator.actionSerial
    LaunchedEffect(pendingAction, actionSerial, readyForNavigation) {
        if (pendingAction == ReopenAction.None || !readyForNavigation) return@LaunchedEffect
        reopenCoordinator.syncTransferState(sender.state.value.transfer, receiver.state.value.transfer)
        val hasActiveReturnTarget = reopenCoordinator.activeReturnTarget != null &&
            (sender.state.value.transfer.isPayloadTransferActive ||
                receiver.state.value.transfer.isPayloadTransferActive)
        if (pendingAction == ReopenAction.RestoreTransfersOngoing && hasActiveReturnTarget) {
            ongoingFocusKey++
            val activeRoute = navController.currentDestination?.route
            val fromProgress = activeRoute == Screen.Send.route || activeRoute == Screen.Receive.route
            selectMain(MainDestination.Transfers, savePoppedState = !fromProgress)
        } else {
            if (nearby.state.value.phase != Phase.Idle || nearby.state.value.notice != null || nearby.state.value.cardNotice != null) {
                nearby.stop()
                nearby.clearNotice()
            }
            sender.resetAbandonedSession()
            receiver.resetAbandonedSession()
            if (navController.currentDestination?.route != Screen.Home.route) {
                if (!navController.popBackStack(Screen.Home.route, inclusive = false)) {
                    selectMain(MainDestination.Home, savePoppedState = false)
                }
            }
        }
        reopenCoordinator.consumeAction(actionSerial)
    }

    LaunchedEffect(backStackEntry) {
        if (backStackEntry != null && selected != null &&
            !sender.state.value.transfer.isPayloadTransferActive &&
            !receiver.state.value.transfer.isPayloadTransferActive
        ) {
            sender.stopWaitingForReceiver()
            if (nearby.state.value.phase != Phase.Idle || nearby.state.value.notice != null || nearby.state.value.cardNotice != null) {
                nearby.stop()
            }
        }
    }

    val colors = MaterialTheme.colorScheme
    val transferRoutes = remember { setOf(Screen.Send.route, Screen.Receive.route) }
    val mainScreenInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    val transferScreenInsets = WindowInsets.systemBars.only(
        WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
    )
    MainNavigationContainer(
        modifier = Modifier.fillMaxSize().background(colors.background),
        showBottomBar = selected != null,
        bottomBar = {
            selected?.let { BottomNavBar(it, ::selectMain) }
        }
    ) { bottomBarHeight ->
        val mainScreenModifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(mainScreenInsets)
            .padding(bottom = bottomBarHeight)
        val transferScreenModifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(transferScreenInsets)
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                if (targetState.destination.route in transferRoutes) fadeIn(tween(180))
                else EnterTransition.None
            },
            exitTransition = {
                if (initialState.destination.route in transferRoutes) fadeOut(tween(180))
                else ExitTransition.None
            },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = {
                if (initialState.destination.route in transferRoutes) fadeOut(tween(180))
                else ExitTransition.None
            }
        ) {
            composable(Screen.Home.route) {
                Box(mainScreenModifier) {
                    HomeScreen(
                        onSend = {
                            if (sender.state.value.transfer?.active == false) {
                                if (sender.state.value.transfer?.status == TransferStatus.Completed) {
                                    sender.finishCompletedTransfer()
                                } else {
                                    sender.closeTransfer()
                                }
                            }
                            navController.navigate(Screen.Send.route) { launchSingleTop = true }
                        },
                        onReceive = {
                            if (receiver.state.value.transfer?.active == false) {
                                receiver.closeTransfer()
                            }
                            navController.navigate(Screen.Receive.route) { launchSingleTop = true }
                        },
                        onSeeAll = { selectMain(MainDestination.Library) }
                    )
                }
            }
            composable(Screen.Library.route) {
                Box(mainScreenModifier) { LibraryScreen() }
            }
            composable(Screen.Transfers.route) {
                Box(mainScreenModifier) {
                    TransfersScreen(sendingState.transfer, receivingState.transfer, ongoingFocusKey) { received ->
                        val route = if (received) Screen.Receive.route else Screen.Send.route
                        navController.navigate(route) {
                            popUpTo(Screen.Transfers.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }
            composable(Screen.Profile.route) {
                Box(mainScreenModifier) { ProfileScreen() }
            }
            composable(Screen.Send.route) { entry ->
                val startNearby = rememberNearbyStart(nearby) { navController.currentBackStackEntry == entry }
                LaunchedEffect(senderSession?.generation, senderSession?.phase, link) {
                    val connected = nearby.state.value
                    val activeLink = app.transport.link.value
                    if (connected.phase == Phase.Idle) sender.stopWaitingForReceiver()
                    if (connected == senderSession && connected.phase == Phase.Connected && activeLink != null) {
                        val peer = checkNotNull(connected.peer)
                        sender.offerConnectedFiles(connected.generation, peer.id, peer.name, activeLink::sender)
                    }
                }
                Box(transferScreenModifier) {
                    SendScreen(
                        session = senderSession,
                        onStopSharing = {
                            sender.stopWaitingForReceiver()
                            if (nearby.state.value.role == NearbyRole.Advertiser) nearby.stop()
                        },
                        onDecision = nearby::decide,
                        model = sender,
                        onBack = { navController.popBackStack() },
                        onAdvertise = if (session.phase == Phase.Idle) {
                            { startNearby(NearbyRole.Advertiser) }
                        } else null,
                        onTransferDone = ::returnHome,
                        onOpenTransfers = ::openTransfersFromProgress,
                        idleNotice = session.cardNotice.takeIf { session.phase == Phase.Idle },
                        onDismissNotice = nearby::clearNotice
                    )
                }
            }
            composable(Screen.Receive.route) { entry ->
                val startNearby = rememberNearbyStart(nearby) { navController.currentBackStackEntry == entry }
                Box(transferScreenModifier) {
                    ReceiveScreen(
                        model = receiver,
                        onTransferDone = ::returnHome,
                        onBack = { navController.popBackStack() },
                        nearby = session,
                        onScan = if (session.phase == Phase.Idle) {
                            { startNearby(NearbyRole.Discoverer) }
                        } else null,
                        onConnect = nearby::request,
                        onStop = nearby::stop,
                        onDecision = nearby::decide,
                        onOpenTransfers = ::openTransfersFromProgress,
                        onDismissNotice = nearby::clearNotice
                    )
                }
            }
        }
    }
}

@Composable
private fun MainNavigationContainer(
    modifier: Modifier = Modifier,
    showBottomBar: Boolean,
    bottomBar: @Composable () -> Unit,
    content: @Composable (bottomBarHeight: Dp) -> Unit
) {
    val density = LocalDensity.current
    SubcomposeLayout(modifier) { constraints ->
        val barPlaceables = if (showBottomBar) {
            subcompose("bottomBar", bottomBar).map {
                it.measure(constraints.copy(minWidth = 0, minHeight = 0))
            }
        } else {
            emptyList()
        }
        val barHeightPx = barPlaceables.maxOfOrNull { it.height } ?: 0
        val barHeightDp = with(density) { barHeightPx.toDp() }
        val contentPlaceables = subcompose("content") {
            content(barHeightDp)
        }.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            barPlaceables.forEach { it.placeRelative(0, constraints.maxHeight - barHeightPx) }
            contentPlaceables.forEach { it.placeRelative(0, 0) }
        }
    }
}
