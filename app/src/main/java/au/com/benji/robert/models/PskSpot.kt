package au.com.benji.robert.models

import kotlinx.serialization.Serializable

@Serializable
data class PskSpot(
    val callsign: String,
    val grid: String,
    val lat: Double,
    val lon: Double,
    val frequency: Double,
    val mode: String,
    val reportTime: Long,
    val distance: Double = 0.0,
    val bearing: Double = 0.0,
    val country: String = "",
    val signal: Int = 0
)
