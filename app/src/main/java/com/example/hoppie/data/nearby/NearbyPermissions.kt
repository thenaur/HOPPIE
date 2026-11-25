package com.example.hoppie.data.nearby

import android.Manifest.permission

internal fun nearbyPermissions(sdk: Int): List<String> = buildList {
    if (sdk <= 32) add(permission.ACCESS_COARSE_LOCATION)
    if (sdk in 29..32) add(permission.ACCESS_FINE_LOCATION)
    if (sdk >= 31) {
        add(permission.BLUETOOTH_SCAN)
        add(permission.BLUETOOTH_CONNECT)
        add(permission.BLUETOOTH_ADVERTISE)
    }
    if (sdk >= 33) add(permission.NEARBY_WIFI_DEVICES)
    if (sdk >= 37) add(permission.ACCESS_LOCAL_NETWORK)
}
