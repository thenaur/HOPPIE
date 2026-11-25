package com.example.hoppie.features.send

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import com.example.hoppie.HoppieApplication
import com.example.hoppie.MainActivity
import com.example.hoppie.data.nearby.Phase
import com.example.hoppie.data.nearby.nearbyPermissions
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class SenderAdvertisingDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun twoSelectedFilesSurviveStopAndSystemBack() {
        val activity = compose.activity
        val app = activity.application as HoppieApplication
        assumeTrue(nearbyPermissions(Build.VERSION.SDK_INT).all {
            activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        })
        val files = List(2) { File.createTempFile("hoppie-share-", ".txt", app.cacheDir).apply { writeText("test") } }
        try {
            compose.onNodeWithText("Send Files").performClick()
            val model = compose.runOnIdle { ViewModelProvider(activity)[SendViewModel::class.java] }
            compose.runOnIdle { model.addFiles(files.map(Uri::fromFile)) }
            compose.waitUntil(5_000) { model.state.value.files.size == 2 }
            for (systemBack in listOf(false, true)) {
                compose.onNodeWithText("Send").performClick()
                compose.waitUntil(15_000) { app.nearby.state.value.phase == Phase.Searching }
                compose.onNodeWithText("Sending Files").assertIsDisplayed()
                compose.onNodeWithText("Sharing files (2)").assertIsDisplayed()
                if (systemBack) compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
                else compose.onNodeWithText("Stop Sharing").performClick()
                compose.onNodeWithText("Selected files (2)").assertIsDisplayed()
                compose.runOnIdle { assertEquals(Phase.Idle, app.nearby.state.value.phase) }
            }
        } finally {
            compose.runOnIdle { app.nearby.stop() }
            files.forEach { it.delete() }
        }
    }
}
