package au.com.benji.robert.utils

import java.time.ZonedDateTime
import java.time.ZoneId
import java.util.Calendar
import kotlin.math.*

/**
 * Advanced solar positioning for propagation modeling.
 */
object SolarCalculations {

    /**
     * Calculates solar elevation angle.
     * simplified version of the algorithm.
     */
    fun getSolarElevation(lat: Double, lon: Double): Double {
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val dayOfYear = now.dayOfYear
        val hour = now.hour + (now.minute / 60.0)
        
        val declination = 23.45 * sin(Math.toRadians(360.0 / 365.0 * (dayOfYear - 81)))
        
        val timeCorrection = 4 * (lon - (15 * 0)) // 0 for UTC
        val solarTime = hour + (timeCorrection / 60.0)
        val hourAngle = 15 * (solarTime - 12)
        
        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(declination)
        val hrRad = Math.toRadians(hourAngle)
        
        val sinElevation = sin(latRad) * sin(decRad) + cos(latRad) * cos(decRad) * cos(hrRad)
        return Math.toDegrees(asin(sinElevation))
    }

    /**
     * Returns true if within 45 mins of sunrise or sunset.
     */
    fun isGreyline(lat: Double, lon: Double): Boolean {
        val elevation = getSolarElevation(lat, lon)
        // Greyline is approximately when the sun is between -6 and +6 degrees
        return elevation in -8.0..8.0
    }
}
