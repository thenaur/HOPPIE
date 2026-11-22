package com.example.hoppie.features.receive

internal sealed interface DiscoveryStatus {
    data object Starting : DiscoveryStatus
    data object Scanning : DiscoveryStatus
    data object Completed : DiscoveryStatus
    data class Unavailable(val message: String) : DiscoveryStatus
}

internal sealed interface DeviceConnection {
    data object Available : DeviceConnection
    data object Connecting : DeviceConnection
    data object Connected : DeviceConnection
    data class Failed(val message: String) : DeviceConnection
}

internal data class NearbyDevice(
    val id: String,
    val name: String,
    val connection: DeviceConnection = DeviceConnection.Available
)

internal data class ReceiveUiState(
    val discovery: DiscoveryStatus,
    val devices: List<NearbyDevice> = emptyList()
)
