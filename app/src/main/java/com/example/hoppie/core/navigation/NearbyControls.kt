package com.example.hoppie.core.navigation

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.hoppie.data.nearby.NearbyRole
import com.example.hoppie.data.nearby.NearbySession
import com.example.hoppie.data.nearby.Phase
import com.example.hoppie.data.nearby.nearbyPermissions

internal val LocalSuppressReopen = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

@Composable
internal fun rememberNearbyStart(session: NearbySession, isCurrentDestination: () -> Boolean): (NearbyRole) -> Unit {
    val context = LocalContext.current
    val suppressReopen = LocalSuppressReopen.current
    var pending by rememberSaveable { mutableStateOf<NearbyRole?>(null) }
    val required = nearbyPermissions(Build.VERSION.SDK_INT)
    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val role = pending
        pending = null
        if (role != null && isCurrentDestination()) {
            if (required.all(::granted)) session.start(role) else session.permissionDenied()
        }
    }
    return { role ->
        if (isCurrentDestination() && session.state.value.phase == Phase.Idle && pending == null) {
            if (required.all(::granted)) session.start(role)
            else {
                pending = role
                suppressReopen(true)
                launcher.launch(required.toTypedArray())
            }
        }
    }
}
