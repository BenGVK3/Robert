package au.com.benji.robert.models

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class DxSpot(
    val callsign: String,
    val frequency: String,
    val mode: String = "",
    val spotter: String = "",
    val timestamp: Long = System.currentTimeMillis(), // Store absolute time in ms
    val comment: String = "",
    val source: SpotSource = SpotSource.DX_CLUSTER,
    val location: String = ""
) {
    // Computed property for Zulu time (HH:mmz)
    val timeZulu: String
        get() {
            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC"))
            return zdt.format(DateTimeFormatter.ofPattern("HH:mm")) + "z"
        }

    // Computed property for Local time (HH:mm)
    val timeLocal: String
        get() {
            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
            return zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
}

enum class SpotSource {
    DX_CLUSTER,
    POTA,
    SOTA,
    WWFF
}
