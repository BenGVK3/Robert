package au.com.benji.robert.utils

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
