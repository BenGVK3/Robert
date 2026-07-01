package au.com.benji.robert.utils

import java.util.*
import kotlin.math.*

object TerminatorUtils {

    /**
     * Calculates the points for the day/night terminator.
     * Returns a list of [Pair<Double, Double>] representing (latitude, longitude).
     */
    fun calculateTerminator(date: Date = Date()): List<Pair<Double, Double>> {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = date
        
        val julianDay = getJulianDay(date)
        val gst = getGreenwichSiderealTime(julianDay)
        
        // Solar coordinates
        val sunCoords = getSunCoordinates(julianDay)
        val sunDeclination = sunCoords.first
        val sunRightAscension = sunCoords.second

        val points = mutableListOf<Pair<Double, Double>>()
        
        // Calculate longitude for each latitude
        for (i in -90..90 step 2) {
            val lat = i.toDouble()
            val latRad = Math.toRadians(lat)
            val decRad = Math.toRadians(sunDeclination)

            // The terminator is where the sun's altitude is 0
            // cos(h) = (sin(alt) - sin(lat)*sin(dec)) / (cos(lat)*cos(dec))
            // with alt = 0, sin(alt) = 0
            val cosH = -tan(latRad) * tan(decRad)
            
            if (cosH >= 1.0) {
                // Constant night or day
                continue
            } else if (cosH <= -1.0) {
                // Constant night or day
                continue
            }
            
            val h = Math.toDegrees(acos(cosH))
            
            // Local sidereal time for sunrise/sunset
            val lst1 = sunRightAscension + h
            val lst2 = sunRightAscension - h
            
            // Longitude = LST - GST
            val lon1 = normalizeLongitude(lst1 - gst)
            val lon2 = normalizeLongitude(lst2 - gst)
            
            points.add(Pair(lat, lon1))
            points.add(Pair(lat, lon2))
        }
        
        // Sort points to form a continuous line around the globe
        return points.sortedBy { it.second }
    }

    private fun getJulianDay(date: Date): Double {
        return (date.time / 86400000.0) + 2440587.5
    }

    private fun getGreenwichSiderealTime(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        var gmst = 280.46061837 + 360.98564736629 * (jd - 2451545.0) + 0.000387933 * t * t - t * t * t / 38710000.0
        return normalizeDegrees(gmst)
    }

    private fun getSunCoordinates(jd: Double): Pair<Double, Double> {
        val t = (jd - 2451545.0) / 36525.0
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
        val e = 0.016708634 - 0.000042037 * t - 0.0000001267 * t * t
        
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(Math.toRadians(m)) +
                (0.019993 - 0.000101 * t) * sin(Math.toRadians(2 * m)) +
                0.000289 * sin(Math.toRadians(3 * m))
        
        val trueLong = l0 + c
        val epsilon = 23.439291 - 0.013004167 * t - 0.00000164 * t * t + 0.0000005036 * t * t * t
        
        val ra = Math.toDegrees(atan2(cos(Math.toRadians(epsilon)) * sin(Math.toRadians(trueLong)), cos(Math.toRadians(trueLong))))
        val dec = Math.toDegrees(asin(sin(Math.toRadians(epsilon)) * sin(Math.toRadians(trueLong))))
        
        return Pair(dec, normalizeDegrees(ra))
    }

    private fun normalizeDegrees(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun normalizeLongitude(lon: Double): Double {
        var l = lon % 360.0
        if (l > 180) l -= 360
        if (l <= -180) l += 360
        return l
    }
}
