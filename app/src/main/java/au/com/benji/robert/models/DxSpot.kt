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

    val band: String
        get() {
            val freq = frequency.toDoubleOrNull() ?: return "Unknown"
            return when {
                freq in 1.8..2.0 -> "160m"
                freq in 3.5..4.0 -> "80m"
                freq in 5.3..5.4 -> "60m"
                freq in 7.0..7.3 -> "40m"
                freq in 10.1..10.15 -> "30m"
                freq in 14.0..14.35 -> "20m"
                freq in 18.068..18.168 -> "17m"
                freq in 21.0..21.45 -> "15m"
                freq in 24.89..24.99 -> "12m"
                freq in 28.0..29.7 -> "10m"
                freq in 50.0..54.0 -> "6m"
                freq in 144.0..148.0 -> "2m"
                freq in 430.0..450.0 -> "70cm"
                else -> "Other"
            }
        }

    val normalizedMode: String
        get() = when {
            mode.contains("CW", ignoreCase = true) -> "CW"
            mode.contains("SSB", ignoreCase = true) || mode.contains("LSB", ignoreCase = true) || mode.contains("USB", ignoreCase = true) -> "SSB"
            mode.contains("FM", ignoreCase = true) -> "FM"
            mode.contains("FT8", ignoreCase = true) -> "FT8"
            mode.contains("FT4", ignoreCase = true) -> "FT4"
            mode.contains("JS8", ignoreCase = true) -> "JS8"
            mode.contains("AM", ignoreCase = true) -> "AM"
            mode.contains("RTTY", ignoreCase = true) -> "RTTY"
            mode.contains("PKT", ignoreCase = true) -> "PACKET"
            else -> mode.ifBlank { "VAR" }
        }

    val continent: String
        get() {
            // Very basic prefix to continent mapping
            val call = callsign.uppercase()
            return when {
                call.startsWith("VK") || call.startsWith("ZL") || call.startsWith("YB") -> "OC"
                call.startsWith("W") || call.startsWith("K") || call.startsWith("N") || call.startsWith("AA") || call.startsWith("VE") || call.startsWith("VA") -> "NA"
                call.startsWith("PY") || call.startsWith("LU") || call.startsWith("CX") -> "SA"
                call.startsWith("G") || call.startsWith("M") || call.startsWith("F") || call.startsWith("DL") || call.startsWith("I") || call.startsWith("EA") -> "EU"
                call.startsWith("JA") || call.startsWith("HL") || call.startsWith("BY") -> "AS"
                call.startsWith("ZS") || call.startsWith("5Z") || call.startsWith("6W") -> "AF"
                else -> "UNK"
            }
        }
}

enum class SpotSource {
    DX_CLUSTER,
    POTA,
    SOTA,
    WWFF
}
