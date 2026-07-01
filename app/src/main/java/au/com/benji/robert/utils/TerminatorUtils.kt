package au.com.benji.robert.utils

import java.util.*
import kotlin.math.*

object TerminatorUtils {

    /**
     * Calculates the points for the day/night terminator.
     * Returns a list of [Pair<Double, Double>] representing (latitude, longitude).
     * The points form a loop around the globe.
     */
    fun calculateTerminator(date: Date = Date()): List<Pair<Double, Double>> {
        val julianDay = getJulianDay(date)
        val gst = getGreenwichSiderealTime(julianDay)
        
        val sunCoords = getSunCoordinates(julianDay)
        val sunDec = Math.toRadians(sunCoords.first)
        val sunRA = sunCoords.second

        val points = mutableListOf<Pair<Double, Double>>()
        
        // Calculate points from -90 to 90 latitude
        // We'll calculate both sunrise and sunset longitudes
        val sunrisePoints = mutableListOf<Pair<Double, Double>>()
        val sunsetPoints = mutableListOf<Pair<Double, Double>>()

        for (i in -90..90 step 1) {
            val lat = i.toDouble()
            val latRad = Math.toRadians(lat)

            val cosH = -tan(latRad) * tan(sunDec)
            
            if (cosH >= 1.0) {
                // Constant night (winter pole)
                continue
            } else if (cosH <= -1.0) {
                // Constant day (summer pole)
                continue
            }
            
            val h = Math.toDegrees(acos(cosH))
            
            val lon1 = normalizeLongitude(sunRA + h - gst)
            val lon2 = normalizeLongitude(sunRA - h - gst)
            
            sunrisePoints.add(Pair(lat, lon1))
            sunsetPoints.add(Pair(lat, lon2))
        }
        
        // Combine to form a loop
        points.addAll(sunrisePoints)
        points.addAll(sunsetPoints.reversed())
        
        return points
    }

    private fun getJulianDay(date: Date): Double {
        return (date.time / 86400000.0) + 2440587.5
    }

    private fun getGreenwichSiderealTime(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val gmst = 280.46061837 + 360.98564736629 * (jd - 2451545.0) + 0.000387933 * t * t - t * t * t / 38710000.0
        return normalizeDegrees(gmst)
    }

    private fun getSunCoordinates(jd: Double): Pair<Double, Double> {
        val t = (jd - 2451545.0) / 36525.0
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
        
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
