package com.example.hoppie

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.hoppie.core.navigation.AppNavGraph
import com.example.hoppie.core.theme.HOPPIETheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBars(isSend = false)
        setContent {
            HOPPIETheme {
                AppNavGraph(onSendVisibilityChanged = ::applySystemBars)
            }
        }
    }

    private fun applySystemBars(isSend: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, 0xFF302078.toInt())
        )
        if (isSend && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }
}
