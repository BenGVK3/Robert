package au.com.benji.robert.utils

object HamUtils {
    data class DxccInfo(
        val country: String,
        val prefix: String,
        val continent: String,
        val cqZone: Int,
        val ituZone: Int,
        val flagEmoji: String = ""
    )

    private val prefixMap = mapOf(
        "VK" to DxccInfo("Australia", "VK", "OC", 30, 59, "🇦🇺"),
        "W" to DxccInfo("USA", "W", "NA", 3, 6, "🇺🇸"),
        "K" to DxccInfo("USA", "K", "NA", 3, 6, "🇺🇸"),
        "N" to DxccInfo("USA", "N", "NA", 3, 6, "🇺🇸"),
        "A" to DxccInfo("USA", "A", "NA", 3, 6, "🇺🇸"),
        "G" to DxccInfo("England", "G", "EU", 14, 27, "🇬🇧"),
        "M" to DxccInfo("England", "M", "EU", 14, 27, "🇬🇧"),
        "2" to DxccInfo("England", "2", "EU", 14, 27, "🇬🇧"),
        "JA" to DxccInfo("Japan", "JA", "AS", 25, 45, "🇯🇵"),
        "F" to DxccInfo("France", "F", "EU", 14, 27, "🇫🇷"),
        "I" to DxccInfo("Italy", "I", "EU", 15, 28, "🇮🇹"),
        "D" to DxccInfo("Germany", "D", "EU", 14, 28, "🇩🇪"),
        "VE" to DxccInfo("Canada", "VE", "NA", 4, 9, "🇨🇦"),
        "VA" to DxccInfo("Canada", "VA", "NA", 4, 9, "🇨🇦"),
        "ZL" to DxccInfo("New Zealand", "ZL", "OC", 32, 60, "🇳🇿"),
        "BY" to DxccInfo("China", "BY", "AS", 24, 44, "🇨🇳"),
        "UA" to DxccInfo("Russia", "UA", "EU", 16, 29, "🇷🇺"),
        "PY" to DxccInfo("Brazil", "PY", "SA", 11, 15, "🇧🇷"),
        "LU" to DxccInfo("Argentina", "LU", "SA", 13, 16, "🇦🇷"),
        "ZS" to DxccInfo("South Africa", "ZS", "AF", 38, 57, "🇿🇦")
    )

    fun getDxccInfo(callsign: String): DxccInfo? {
        val upper = callsign.uppercase().trim()
        if (upper.isEmpty()) return null
        
        // Try 2-char prefix
        if (upper.length >= 2) {
            prefixMap[upper.take(2)]?.let { return it }
        }
        
        // Try 1-char prefix
        return prefixMap[upper.take(1)]
    }
}
