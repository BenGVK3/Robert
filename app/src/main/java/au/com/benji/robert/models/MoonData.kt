package au.com.benji.robert.models

import kotlinx.serialization.Serializable

@Serializable
data class MoonData(
    val phaseName: String = "---",
    val phaseIcon: String = "🌑",
    val illumination: Int = 0,
    val age: Double = 0.0,
    val distanceKm: Double = 0.0,
    val angularSize: Double = 0.0,
    val constellation: String = "---",
    val altitude: Double = 0.0,
    val azimuth: Double = 0.0,
    val riseTime: String = "--:--",
    val setTime: String = "--:--",
    val transitTime: String = "--:--",
    val isVisible: Boolean = false,
    val declination: Double = 0.0,
    val radialVelocity: Double = 0.0,
    val doppler144: Double = 0.0,
    val doppler432: Double = 0.0,
    val doppler1296: Double = 0.0,
    val pathLoss: Double = 0.0,
    val oneWayDelay: Double = 0.0,
    val roundTripDelay: Double = 0.0,
    val skyTemp: Double = 0.0,
    val polarization: Double = 0.0,
    val nextFullMoon: String = "---",
    val nextNewMoon: String = "---",
    val nextFirstQuarter: String = "---",
    val nextLastQuarter: String = "---",
    val emeRating: String = "Poor",
    val lastUpdated: Long = 0L
)
