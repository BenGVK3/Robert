package au.com.benji.robert.repository.dx

import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.util.Locale

object DxUtils {
    fun parseIsoToTimestamp(isoString: String): Long {
        if (isoString.isEmpty()) return System.currentTimeMillis()
        return try {
            val cleaned = if (isoString.contains(" ") && !isoString.contains("T")) {
                isoString.replace(" ", "T")
            } else isoString
            val finalString = if (!cleaned.contains("+") && !cleaned.endsWith("Z")) {
                cleaned + "Z"
            } else cleaned
            ZonedDateTime.parse(finalString).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
