package com.example.hoppie.core.navigation

import com.example.hoppie.features.send.FileTransferState
import com.example.hoppie.features.send.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppReopenCoordinatorTest {
    @Test
    fun coldStartHasNoPendingReopenOrReturnTarget() {
        val coordinator = AppReopenCoordinator()
        coordinator.onActivityStarted()
        coordinator.onActivityResumed()

        assertNull(coordinator.activeReturnTarget)
        assertEquals(ReopenAction.None, coordinator.pendingAction)
        assertEquals(0, coordinator.actionSerial)
    }

    @Test
    fun configChangeStopAndCloseDoNotTriggerReopenOrClearSuppression() {
        val coordinator = AppReopenCoordinator()
        coordinator.onActivityStarted()
        coordinator.onActivityResumed()

        coordinator.onActivityStopped(isChangingConfigurations = true)
        coordinator.onTaskOrActivityClosing(isChangingConfigurations = true)
        coordinator.onActivityStarted()

        assertEquals(ReopenAction.None, coordinator.pendingAction)
        assertEquals(0, coordinator.actionSerial)
    }

    @Test
    fun temporaryBackgroundWithoutActiveTransferRequestsReturnHome() {
        val coordinator = AppReopenCoordinator()
        coordinator.syncTransferState(
            sending = FileTransferState(emptyList(), TransferStatus.Preparing),
            receiving = null
        )

        coordinator.onActivityStopped(isChangingConfigurations = false)
        coordinator.onActivityStarted()

        assertEquals(ReopenAction.ReturnHome, coordinator.pendingAction)
        assertEquals(1, coordinator.actionSerial)

        coordinator.consumeAction(1)
        assertEquals(ReopenAction.None, coordinator.pendingAction)
    }

    @Test
    fun suppressedExternalLaunchDoesNotDispatchReopenOnReturn() {
        val coordinator = AppReopenCoordinator()
        coordinator.setSuppressNextBackground(true)
        coordinator.onActivityStopped(isChangingConfigurations = false)
        coordinator.onActivityStarted()
        coordinator.onActivityResumed()

        assertEquals(ReopenAction.None, coordinator.pendingAction)
        assertEquals(0, coordinator.actionSerial)

        // Subsequent real backgrounding works normally once resumed clears suppression.
        coordinator.onActivityStopped(isChangingConfigurations = false)
        coordinator.onActivityStarted()
        assertEquals(ReopenAction.ReturnHome, coordinator.pendingAction)
        assertEquals(1, coordinator.actionSerial)
    }

    @Test
    fun failedSuppressionRollbackAllowsSubsequentBackgroundDetection() {
        val coordinator = AppReopenCoordinator()
        coordinator.setSuppressNextBackground(true)
        coordinator.setSuppressNextBackground(false)
        coordinator.onActivityStopped(isChangingConfigurations = false)
        coordinator.onActivityStarted()

        assertEquals(ReopenAction.ReturnHome, coordinator.pendingAction)
    }

    @Test
    fun nonConfigCloseClearsPendingActionAndPreventsStaleReopen() {
        val coordinator = AppReopenCoordinator()
        coordinator.onActivityStopped(isChangingConfigurations = false)
        coordinator.onTaskOrActivityClosing(isChangingConfigurations = false)
        coordinator.onActivityStarted()

        assertNull(coordinator.activeReturnTarget)
        assertEquals(ReopenAction.None, coordinator.pendingAction)
    }
}
