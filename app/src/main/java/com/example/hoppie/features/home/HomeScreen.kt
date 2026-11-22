package com.example.hoppie.features.home

import android.widget.Toast
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.hoppie.features.home.components.HomeHeader
import com.example.hoppie.features.home.components.HomeMascot
import com.example.hoppie.features.home.components.RecentFilesSection
import com.example.hoppie.features.home.components.SendReceiveButtons

@Composable
fun HomeScreen(onSend: () -> Unit, onReceive: () -> Unit, onSeeAll: () -> Unit) {
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val mascotHeight = (maxHeight * 0.42f).coerceIn(200.dp, 320.dp)
        val heroSpacing = (maxHeight * 0.045f).coerceIn(10.dp, 32.dp)
        Column(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeHeader {
                Toast.makeText(context, "Notifications coming soon", Toast.LENGTH_SHORT).show()
            }
            Spacer(Modifier.height(heroSpacing))
            HomeMascot(Modifier.fillMaxWidth().height(mascotHeight))
            Spacer(Modifier.height(9.dp))
            SendReceiveButtons(onSend, onReceive, Modifier.widthIn(max = 380.dp).fillMaxWidth(0.80f))
            Spacer(Modifier.height(14.dp))
            RecentFilesSection(
                sampleRecentFiles,
                onSeeAll,
                Modifier.widthIn(max = 440.dp).fillMaxWidth(0.92f)
            )
            Spacer(Modifier.height(7.dp))
        }
    }
}
