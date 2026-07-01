package au.com.benji.robert.utils

import com.google.android.gms.maps.model.LatLng
import java.util.*
import kotlin.math.*

object TerminatorUtils {

    fun calculateTerminator(date: Date = Date()): List<LatLng> {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = date
        
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val utcTime = hour + minute / 60.0

        // Solar declination (approximate)
        val declination = 23.45 * sin(Math.toRadians(360.0 / 365.0 * (dayOfYear - 81)))
        val declinationRad = Math.toRadians(declination)

        // Equation of time (approximate, in minutes)
        val b = Math.toRadians(360.0 / 365.0 * (dayOfYear - 81))
        val equationOfTime = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
        
        // Solar noon in UTC
        val solarNoon = 12.0 - (equationOfTime / 60.0)

        val points = mutableListOf<LatLng>()
        for (lat in -90..90 step 2) {
            val latRad = Math.toRadians(lat.toDouble())
            
            // Hour angle at sunset/sunrise
            val cosH = -tan(latRad) * tan(declinationRad)
            
            if (cosH <= -1.0) {
                // All day or all night (Polar regions)
                continue
            } else if (cosH >= 1.0) {
                // All day or all night
                continue
            }
            
            val h = Math.toDegrees(acos(cosH))
            
            // Longitude of sunset and sunrise
            val lon1 = (solarNoon - utcTime) * 15.0 + h
            val lon2 = (solarNoon - utcTime) * 15.0 - h
            
            points.add(LatLng(lat.toDouble(), normalizeLongitude(lon1)))
            points.add(LatLng(lat.toDouble(), normalizeLongitude(lon2)))
        }
        
        // Sort points to draw a proper polygon or line
        // This is tricky because it's a loop. 
        // For simplicity, we can return the points and let the caller handle it.
        return points.sortedBy { it.longitude }
    }

    private fun normalizeLongitude(lon: Double): Double {
        var l = lon
        while (l <= -180) l += 360
        while (l > 180) l -= 360
        return l
    }
}
