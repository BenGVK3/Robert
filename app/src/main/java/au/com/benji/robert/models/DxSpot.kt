package au.com.benji.robert.models

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Serializable
enum class SpotSource {
    SOTA,
    POTA,
    WWFF,
    DX_CLUSTER,
    RBN,
    DIGITAL, // PSK Reporter etc
    SIOTA,
    PARKSNPEAKS
}

@Serializable
data class DxSpot(
    val id: String = "", // Unique ID if available
    val provider: SpotSource,
    val activator: String = "", // The person activating (for SOTA/POTA)
    val callsign: String,      // The station being spotted
    val reference: String = "",  // Summit/Park Reference (e.g. VK3/VC-001)
    val name: String = "",       // Summit/Park Name
    val country: String = "",
    val frequency: String,      // in MHz
    val mode: String = "",
    val spotter: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val comment: String = "",
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val spotUrl: String = ""
) {
    val timeZulu: String
        get() {
            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC"))
            return zdt.format(DateTimeFormatter.ofPattern("HH:mm")) + "z"
        }

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

    val continent: String
        get() {
            val call = callsign.uppercase()
            return when {
                call.startsWith("VK") || call.startsWith("AX") || call.startsWith("ZL") || 
                call.startsWith("ZM") || call.startsWith("YB") || call.startsWith("DU") ||
                call.startsWith("KH6") || call.startsWith("KH7") -> "OC"
                
                call.startsWith("JA") || call.startsWith("BY") || call.startsWith("HL") || 
                call.startsWith("HS") || call.startsWith("9V") || call.startsWith("VU") ||
                call.startsWith("4X") || call.startsWith("HZ") -> "AS"

                call.startsWith("PY") || call.startsWith("LU") || call.startsWith("CE") || 
                call.startsWith("CX") || call.startsWith("YV") || call.startsWith("HK") -> "SA"

                call.startsWith("ZS") || call.startsWith("CN") || call.startsWith("SU") || 
                call.startsWith("7X") || call.startsWith("5Z") || call.startsWith("EL") -> "AF"

                call.startsWith("W") || call.startsWith("K") || call.startsWith("N") || 
                call.startsWith("A") || call.startsWith("VE") || call.startsWith("XE") -> "NA"

                call.startsWith("G") || call.startsWith("F") || call.startsWith("DL") || 
                call.startsWith("I") || call.startsWith("EA") || call.startsWith("PA") ||
                call.startsWith("LA") || call.startsWith("SM") || call.startsWith("OH") ||
                call.startsWith("SP") || call.startsWith("OK") || call.startsWith("HA") ||
                call.startsWith("SV") || call.startsWith("UR") -> "EU"

                else -> "UNK"
            }
        }

    val normalizedMode: String
        get() = when (mode.uppercase()) {
            "CW", "C" -> "CW"
            "SSB", "USB", "LSB", "J3E" -> "SSB"
            "FT8", "FT4", "F8", "F4" -> "FT8/4"
            "RTTY", "RY", "F1B" -> "RTTY"
            "FM", "F3E" -> "FM"
            "DATA", "DIGI", "DIGITAL" -> "DIGI"
            else -> mode.ifEmpty { "---" }
        }
}
