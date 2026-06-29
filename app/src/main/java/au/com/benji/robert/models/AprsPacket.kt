package au.com.benji.robert.models

data class AprsPacket(
    val callsign: String,
    val frequency: String,
    val comment: String,
    val timestamp: Long,
    val symbol: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
