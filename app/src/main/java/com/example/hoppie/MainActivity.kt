package com.example.hoppie

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.example.hoppie.core.navigation.AppNavGraph
import com.example.hoppie.core.navigation.AppReopenCoordinator
import com.example.hoppie.core.navigation.LocalSuppressReopen
import com.example.hoppie.core.theme.HOPPIETheme

class MainActivity : ComponentActivity() {
    private val reopenCoordinator: AppReopenCoordinator by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, 0xFF302078.toInt())
        )
        setContent {
            HOPPIETheme {
                CompositionLocalProvider(
                    LocalSuppressReopen provides reopenCoordinator::setSuppressNextBackground
                ) {
                    AppNavGraph(onSendVisibilityChanged = ::applySystemBars)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            reopenCoordinator.onLauncherReentry()
        }
    }

    override fun onStart() {
        super.onStart()
        reopenCoordinator.onActivityStarted()
    }

    override fun onResume() {
        super.onResume()
        reopenCoordinator.onActivityResumed()
    }

    override fun onStop() {
        super.onStop()
        reopenCoordinator.onActivityStopped(isChangingConfigurations)
    }

    override fun onDestroy() {
        super.onDestroy()
        reopenCoordinator.onTaskOrActivityClosing(isChangingConfigurations)
        if (!isChangingConfigurations) {
            (application as HoppieApplication).nearby.apply {
                stop()
                clearNotice()
            }
        }
    }

    private fun applySystemBars(isSend: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = !isSend
        }
    }
}
