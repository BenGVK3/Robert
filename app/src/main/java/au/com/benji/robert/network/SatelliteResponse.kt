package au.com.benji.robert.network

import kotlinx.serialization.Serializable

@Serializable
data class SatellitePosition(
    val name: String,
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val velocity: Double,
    val visibility: String = "unknown",
    val timestamp: Long = System.currentTimeMillis(),
    val footprint: Double = 0.0,
    val azimuth: Double = 0.0,
    val elevation: Double = 0.0,
    val isVisible: Boolean = false,
    val distance: Double = 0.0,
    val orbitNumber: Int = 0,
    val inclination: Double = 0.0,
    val isSunlit: Boolean = true,
    val localTime: String = "",
    val gridLocator: String = "",
    val rangeRate: Double = 0.0 // km/s, positive = moving away
)

@Serializable
data class SatellitePass(
    val name: String = "",
    @kotlinx.serialization.SerialName("start") val startTime: Long, // Unix timestamp
    @kotlinx.serialization.SerialName("end") val endTime: Long,
    val maxElevation: Double = 0.0,
    val duration: Long = 0, // Seconds
    val aosAzimuth: Double = 0.0,
    val losAzimuth: Double = 0.0,
    val quality: String = "Fair",
    val isVisible: Boolean = false,
    val isDaylight: Boolean = false,
    val direction: String = "N-S"
)

@Serializable
data class SatelliteCommInfo(
    val downlink: String = "",
    val uplink: String = "",
    val mode: String = "",
    val polarization: String = "Linear",
    val plTone: String = "",
    val doppler2m: String = "",
    val doppler70cm: String = ""
)
