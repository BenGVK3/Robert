package au.com.benji.robert.models

import kotlinx.serialization.Serializable

@Serializable
data class AprsPacket(
    val callsign: String,
    val latitude: Double,
    val longitude: Double,
    val symbol: String = "",
    val comment: String? = null,
    val speed: Double? = null,
    val course: Int? = null,
    val altitude: Double? = null,
    val timestamp: Long,
    val distance: Double = 0.0,
    val bearing: Double = 0.0
)
