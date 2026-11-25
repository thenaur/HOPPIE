package com.example.hoppie

import android.app.Application
import com.example.hoppie.data.nearby.GoogleNearbyTransport
import com.example.hoppie.data.nearby.NearbySession

class HoppieApplication : Application() {
    internal val transport: GoogleNearbyTransport by lazy { GoogleNearbyTransport(this) { nearby.state.value } }
    internal val nearby: NearbySession by lazy { NearbySession(transport) }
}
