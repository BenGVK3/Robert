package au.com.benji.robert.utils

import kotlin.math.*

fun calculateMaidenhead(lat: Double, lon: Double): String {
    val longitude = lon + 180
    val latitude = lat + 90

    val lonField = (longitude / 20).toInt().coerceIn(0, 17)
    val latField = (latitude / 10).toInt().coerceIn(0, 17)

    val lonSquare = ((longitude % 20) / 2).toInt().coerceIn(0, 9)
    val latSquare = (latitude % 10).toInt().coerceIn(0, 9)

    val lonSub = (((longitude % 2) / (2.0 / 24.0))).toInt().coerceIn(0, 23)
    val latSub = ((latitude % 1) / (1.0 / 24.0)).toInt().coerceIn(0, 23)

    return "${('A' + lonField)}${('A' + latField)}$lonSquare$latSquare${('a' + lonSub)}${('a' + latSub)}"
}

fun maidenheadToLatLng(locator: String): Pair<Double, Double>? {
    val clean = locator.trim().uppercase()
    if (clean.length < 2) return null

    var lon = -180.0
    var lat = -90.0

    // Fields
    lon += (clean[0] - 'A') * 20.0
    lat += (clean[1] - 'A') * 10.0

    if (clean.length >= 4) {
        // Squares
        lon += (clean[2] - '0') * 2.0
        lat += (clean[3] - '0') * 1.0

        if (clean.length >= 6) {
            // Sub-squares
            lon += (clean[4] - 'A') * (2.0 / 24.0)
            lat += (clean[5] - 'A') * (1.0 / 24.0)

            if (clean.length >= 8) {
                // Extended squares
                lon += (clean[6] - '0') * (2.0 / 240.0)
                lat += (clean[7] - '0') * (1.0 / 240.0)
                
                // Centre of 8-char square
                lon += (1.0 / 240.0)
                lat += (0.5 / 240.0)
            } else {
                // Centre of 6-char square
                lon += (1.0 / 24.0)
                lat += (0.5 / 24.0)
            }
        } else {
            // Centre of 4-char square
            lon += 1.0
            lat += 0.5
        }
    } else {
        // Centre of 2-char field
        lon += 10.0
        lat += 5.0
    }

    return Pair(lat, lon)
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val l1 = Math.toRadians(lat1)
    val l2 = Math.toRadians(lat2)
    val dl = Math.toRadians(lon2 - lon1)
    val y = sin(dl) * cos(l2)
    val x = cos(l1) * sin(l2) - sin(l1) * cos(l2) * cos(dl)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}

fun getCompassDirection(bearing: Double): String {
    val directions = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = (((bearing + 11.25) % 360) / 22.5).toInt()
    return directions[index % 16]
}
