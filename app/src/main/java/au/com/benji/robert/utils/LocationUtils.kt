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
