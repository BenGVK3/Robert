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
            val call = callsign.uppercase()
            
            // Comprehensive prefix to continent mapping
            return when {
                // Oceania (OC)
                call.startsWith("VK") || call.startsWith("AX") || // Australia
                call.startsWith("ZL") || call.startsWith("ZM") || // New Zealand
                call.startsWith("YB") || call.startsWith("YC") || call.startsWith("YD") || call.startsWith("YE") || // Indonesia
                call.startsWith("DU") || call.startsWith("DV") || call.startsWith("DZ") || // Philippines
                call.startsWith("V7") || call.startsWith("V8") || call.startsWith("V6") || 
                call.startsWith("FK") || call.startsWith("FO") || call.startsWith("FW") || 
                call.startsWith("KH6") || call.startsWith("KH7") || // Hawaii (OC)
                call.startsWith("H4") || call.startsWith("P2") || call.startsWith("3D2") || 
                call.startsWith("5W") || call.startsWith("E5") || call.startsWith("YJ") || 
                call.startsWith("TX") || call.startsWith("T2") || call.startsWith("T3") -> "OC"

                // Asia (AS)
                call.startsWith("JA") || call.startsWith("7J") || call.startsWith("8J") || // Japan
                call.startsWith("BY") || call.startsWith("BT") || call.startsWith("BZ") || // China
                call.startsWith("HL") || call.startsWith("DS") || call.startsWith("DT") || // South Korea
                call.startsWith("RA") || call.startsWith("RC") || call.startsWith("RU") || // Russia (Most are AS/EU, simplified)
                call.startsWith("HS") || call.startsWith("E2") || // Thailand
                call.startsWith("9V") || call.startsWith("9M") || // Singapore/Malaysia
                call.startsWith("VU") || call.startsWith("AT") || // India
                call.startsWith("A4") || call.startsWith("A6") || call.startsWith("A7") || // Middle East
                call.startsWith("4X") || call.startsWith("4Z") || // Israel
                call.startsWith("EP") || call.startsWith("HZ") || call.startsWith("JY") || 
                call.startsWith("VR") || call.startsWith("XX9") || call.startsWith("BV") || 
                call.startsWith("XV") || call.startsWith("XU") || call.startsWith("XW") -> "AS"

                // South America (SA)
                call.startsWith("PY") || call.startsWith("PP") || call.startsWith("PU") || // Brazil
                call.startsWith("LU") || call.startsWith("LW") || call.startsWith("AY") || // Argentina
                call.startsWith("CE") || call.startsWith("XQ") || call.startsWith("XR") || // Chile
                call.startsWith("CX") || call.startsWith("CV") || // Uruguay
                call.startsWith("YV") || call.startsWith("YY") || // Venezuela
                call.startsWith("HK") || call.startsWith("HJ") || // Colombia
                call.startsWith("OA") || call.startsWith("OB") || // Peru
                call.startsWith("HC") || call.startsWith("HD") || // Ecuador
                call.startsWith("ZP") || call.startsWith("CP") -> "SA"

                // Africa (AF)
                call.startsWith("ZS") || call.startsWith("ZR") || call.startsWith("ZT") || // South Africa
                call.startsWith("CN") || call.startsWith("5C") || // Morocco
                call.startsWith("SU") || call.startsWith("ST") || // Egypt/Sudan
                call.startsWith("7X") || call.startsWith("7W") || // Algeria
                call.startsWith("EA8") || call.startsWith("EA9") || // Canary Islands / Ceuta
                call.startsWith("5Z") || call.startsWith("5X") || call.startsWith("5H") || // East Africa
                call.startsWith("EL") || call.startsWith("TU") || call.startsWith("9G") || // West Africa
                call.startsWith("V5") || call.startsWith("A2") || call.startsWith("7P") || 
                call.startsWith("D4") || call.startsWith("D6") || call.startsWith("E3") || 
                call.startsWith("E4") || call.startsWith("ET") || call.startsWith("J2") || 
                call.startsWith("S7") || call.startsWith("VQ9") || call.startsWith("3B") -> "AF"

                // North America (NA)
                call.startsWith("W") || call.startsWith("K") || call.startsWith("N") || call.startsWith("A") || // USA
                call.startsWith("VE") || call.startsWith("VA") || call.startsWith("VO") || // Canada
                call.startsWith("XE") || call.startsWith("XF") || // Mexico
                call.startsWith("TG") || call.startsWith("YS") || call.startsWith("HR") || 
                call.startsWith("YN") || call.startsWith("TI") || call.startsWith("HP") || // Central America
                call.startsWith("CM") || call.startsWith("CO") || // Cuba
                call.startsWith("HI") || call.startsWith("HI") || // Dominican Republic
                call.startsWith("KP4") || call.startsWith("WP4") || // Puerto Rico
                call.startsWith("PJ") || call.startsWith("6Y") || call.startsWith("8P") || 
                call.startsWith("V2") || call.startsWith("V3") || call.startsWith("V4") -> "NA"

                // Europe (EU)
                call.startsWith("G") || call.startsWith("M") || call.startsWith("2") || // UK
                call.startsWith("F") || call.startsWith("TK") || // France
                call.startsWith("DL") || call.startsWith("DJ") || call.startsWith("DK") || // Germany
                call.startsWith("I") || call.startsWith("IS0") || // Italy
                call.startsWith("EA") || call.startsWith("EB") || // Spain
                call.startsWith("HB") || call.startsWith("HB0") || // Switzerland
                call.startsWith("ON") || call.startsWith("OO") || // Belgium
                call.startsWith("PA") || call.startsWith("PD") || // Netherlands
                call.startsWith("LA") || call.startsWith("LB") || // Norway
                call.startsWith("SM") || call.startsWith("SA") || // Sweden
                call.startsWith("OH") || call.startsWith("OI") || // Finland
                call.startsWith("OZ") || call.startsWith("OU") || // Denmark
                call.startsWith("SP") || call.startsWith("SQ") || // Poland
                call.startsWith("OK") || call.startsWith("OL") || // Czech Republic
                call.startsWith("HA") || call.startsWith("HG") || // Hungary
                call.startsWith("SV") || call.startsWith("SW") || // Greece
                call.startsWith("YO") || call.startsWith("YP") || // Romania
                call.startsWith("LZ") || call.startsWith("E7") || call.startsWith("S5") || 
                call.startsWith("LY") || call.startsWith("YL") || call.startsWith("ES") || 
                call.startsWith("UR") || call.startsWith("US") || call.startsWith("UT") || // Ukraine
                call.startsWith("CT") || call.startsWith("CU") || // Portugal
                call.startsWith("OE") || call.startsWith("9A") || call.startsWith("YU") -> "EU"

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
