package au.com.benji.robert.network

import kotlinx.serialization.Serializable

@Serializable
data class SatellitePosition(
    val name: String = "ISS",
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val velocity: Double,
    val visibility: String,
    val timestamp: Long
)
