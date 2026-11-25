package com.example.hoppie.core.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.hoppie.features.send.FileTransferState
import com.example.hoppie.features.send.isPayloadTransferActive

internal enum class ActiveTransferRole {
    Sender,
    Receiver
}

internal data class ActiveTransferReturnTarget(
    val role: ActiveTransferRole,
    val fileCount: Int
)

internal enum class ReopenAction {
    None,
    RestoreTransfersOngoing,
    ReturnHome
}

internal class AppReopenCoordinator : ViewModel() {
    var activeReturnTarget by mutableStateOf<ActiveTransferReturnTarget?>(null)
        private set

    var pendingAction by mutableStateOf(ReopenAction.None)
        private set

    var actionSerial by mutableIntStateOf(0)
        private set

    private var temporarilyBackgrounded = false
    private var suppressNextBackground = false

    fun syncTransferState(sending: FileTransferState?, receiving: FileTransferState?) {
        val nextTarget = when {
            sending.isPayloadTransferActive -> ActiveTransferReturnTarget(
                role = ActiveTransferRole.Sender,
                fileCount = sending!!.files.size
            )
            receiving.isPayloadTransferActive -> ActiveTransferReturnTarget(
                role = ActiveTransferRole.Receiver,
                fileCount = receiving!!.files.size
            )
            else -> null
        }
        activeReturnTarget = nextTarget
        if (nextTarget == null && pendingAction == ReopenAction.RestoreTransfersOngoing) {
            pendingAction = ReopenAction.None
        }
    }

    fun setSuppressNextBackground(suppressed: Boolean) {
        suppressNextBackground = suppressed
    }

    fun onActivityStopped(isChangingConfigurations: Boolean) {
        // Keep activeReturnTarget intact on temporary background transitions; only track background entry.
        if (!isChangingConfigurations && !suppressNextBackground) {
            temporarilyBackgrounded = true
        }
    }

    fun onActivityResumed() {
        suppressNextBackground = false
    }

    fun onActivityStarted() {
        if (!temporarilyBackgrounded) return
        temporarilyBackgrounded = false
        dispatchReopenAction()
    }

    fun onLauncherReentry() {
        temporarilyBackgrounded = false
        dispatchReopenAction()
    }

    fun consumeAction(serial: Int) {
        if (serial == actionSerial) {
            pendingAction = ReopenAction.None
        }
    }

    fun onTaskOrActivityClosing(isChangingConfigurations: Boolean) {
        if (isChangingConfigurations) return
        activeReturnTarget = null
        pendingAction = ReopenAction.None
        temporarilyBackgrounded = false
        suppressNextBackground = false
    }

    private fun dispatchReopenAction() {
        pendingAction = if (activeReturnTarget != null) {
            ReopenAction.RestoreTransfersOngoing
        } else {
            ReopenAction.ReturnHome
        }
        actionSerial++
    }
}
