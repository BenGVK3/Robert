package au.com.benji.robert.repository

import android.util.Log
import au.com.benji.robert.database.CacheDao
import au.com.benji.robert.database.SatelliteEntity
import au.com.benji.robert.network.ApiService
import au.com.benji.robert.network.SatelliteCommInfo
import au.com.benji.robert.network.SatellitePosition
import au.com.benji.robert.network.SatellitePass
import au.com.benji.robert.utils.calculateMaidenhead
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

class SatelliteRepository(private val cacheDao: CacheDao) {
    private val TAG = "SatelliteRepository"
    private val json = Json { ignoreUnknownKeys = true }

    private var lastDistance: Double? = null
    private var lastTimestamp: Long = 0

    val availableSatellites = listOf(
        SatelliteMetadata("25544", "ISS (Zarya)", "FM Repeater / APRS / SSTV", "Amateur", 
            SatelliteCommInfo("145.800", "145.990", "FM/APRS", "Circular", "67.0 Hz")),
        SatelliteMetadata("25338", "NOAA 15", "APT Weather", "Weather",
            SatelliteCommInfo("137.620", "-", "APT", "RHCP")),
        SatelliteMetadata("28654", "NOAA 18", "APT Weather", "Weather",
            SatelliteCommInfo("137.912", "-", "APT", "RHCP")),
        SatelliteMetadata("33591", "NOAA 19", "APT Weather", "Weather",
            SatelliteCommInfo("137.100", "-", "APT", "RHCP")),
        SatelliteMetadata("43013", "AO-91", "RadFxSat (Fox-1B)", "Amateur",
            SatelliteCommInfo("145.960", "435.250", "FM", "Linear", "67.0 Hz")),
        SatelliteMetadata("43770", "AO-92", "Fox-1D", "Amateur",
            SatelliteCommInfo("145.880", "435.350", "FM", "Linear", "67.0 Hz")),
        SatelliteMetadata("27607", "SO-50", "Saudi-OSCAR 50", "Amateur",
            SatelliteCommInfo("436.795", "145.850", "FM", "Linear", "67.0 Hz")),
        SatelliteMetadata("53378", "BlueWalker 3", "Direct-to-Cell", "Experimental"),
        SatelliteMetadata("40069", "XW-2A", "Cas-3A", "Amateur",
            SatelliteCommInfo("145.660", "435.030", "SSB/CW")),
        SatelliteMetadata("44443", "FO-99", "NEXUS", "Amateur",
            SatelliteCommInfo("437.075", "145.980", "FM/CW")),
        SatelliteMetadata("48274", "CSS (Tianhe)", "Chinese Space Station", "ISS")
    )

    fun getFavoriteSatelliteIds(): Flow<Set<String>> = 
        cacheDao.getFavoriteSatellites().map { list -> list.map { it.id }.toSet() }

    suspend fun toggleFavorite(id: String) {
        val metadata = availableSatellites.find { it.id == id } ?: return
        val current = cacheDao.getFavoriteSatellites().first().find { it.id == id }
        if (current != null) {
            cacheDao.updateSatellite(current.copy(isFavorite = !current.isFavorite))
        } else {
            cacheDao.insertSatellites(listOf(SatelliteEntity(id, metadata.name, isFavorite = true)))
        }
    }

    fun getPosition(id: String, userLat: Double, userLon: Double): Flow<SatellitePosition?> = flow {
        lastDistance = null
        lastTimestamp = 0

        while (true) {
            try {
                val response = ApiService.fetchData("https://api.wheretheiss.at/v1/satellites/$id")
                response?.let {
                    val raw = json.parseToJsonElement(it).jsonObject
                    
                    val lat = raw["latitude"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    val lon = raw["longitude"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    val alt = raw["altitude"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    val vel = raw["velocity"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    val vis = raw["visibility"]?.jsonPrimitive?.content ?: "unknown"
                    val metadata = availableSatellites.find { s -> s.id == id }
                    val name = metadata?.name ?: "Unknown"

                    val (az, el) = calculateAzEl(userLat, userLon, 0.0, lat, lon, alt)
                    val dist = calculateDistance(userLat, userLon, 0.0, lat, lon, alt)
                    
                    val now = System.currentTimeMillis()
                    var rangeRate = 0.0
                    if (lastDistance != null && lastTimestamp > 0) {
                        val dt = (now - lastTimestamp) / 1000.0
                        if (dt > 0) {
                            rangeRate = (dist - lastDistance!!) / dt
                        }
                    }
                    lastDistance = dist
                    lastTimestamp = now

                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("UTC")

                    val earthRadius = 6371.0
                    val safeAlt = alt.coerceAtLeast(0.0)
                    val acosArg = (earthRadius / (earthRadius + safeAlt)).coerceIn(-1.0, 1.0)
                    val footprintVal = 12756.2 * acos(acosArg)

                    emit(SatellitePosition(
                        name = name,
                        id = id,
                        latitude = lat,
                        longitude = lon,
                        altitude = alt,
                        velocity = vel,
                        visibility = vis,
                        azimuth = az,
                        elevation = el,
                        isVisible = el > 0,
                        footprint = footprintVal,
                        distance = dist,
                        orbitNumber = (raw["orbit"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0).toInt(),
                        inclination = 51.6,
                        isSunlit = vis == "daylight",
                        localTime = sdf.format(Date()),
                        gridLocator = calculateMaidenhead(lat, lon),
                        rangeRate = rangeRate
                    ))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching position: ${e.message}")
            }
            delay(1000) // 1 second update for high performance tracking
        }
    }

    fun getUpcomingPasses(id: String, lat: Double, lon: Double): Flow<List<SatellitePass>> = flow {
        try {
            val url = "https://api.wheretheiss.at/v1/satellites/$id/passes?latitude=$lat&longitude=$lon&days=5"
            val response = ApiService.fetchData(url)
            if (response != null) {
                val now = System.currentTimeMillis() / 1000
                val metadata = availableSatellites.find { s -> s.id == id }
                val mockPasses = listOf(
                    SatellitePass(metadata?.name ?: "", now + 3600, now + 4200, 45.0, 600, 210.0, 30.0, "Excellent", true, true, "SW-NE"),
                    SatellitePass(metadata?.name ?: "", now + 10800, now + 11400, 15.0, 500, 180.0, 90.0, "Fair", false, false, "S-E"),
                    SatellitePass(metadata?.name ?: "", now + 21600, now + 22200, 85.0, 720, 190.0, 10.0, "Excellent", true, false, "S-N")
                )
                emit(mockPasses)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, alt1: Double, lat2: Double, lon2: Double, alt2: Double): Double {
        val re = 6371.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        
        val c = 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        val groundDist = re * c
        
        val vertDist = alt2 - alt1
        return sqrt(groundDist.pow(2) + vertDist.pow(2))
    }

    private fun calculateAzEl(
        obsLat: Double, obsLon: Double, obsAlt: Double,
        satLat: Double, satLon: Double, satAlt: Double
    ): Pair<Double, Double> {
        val re = 6371.0
        val lat1 = Math.toRadians(obsLat)
        val lon1 = Math.toRadians(obsLon)
        val lat2 = Math.toRadians(satLat)
        val lon2 = Math.toRadians(satLon)
        val r1 = re + obsAlt
        val r2 = re + satAlt
        val x1 = r1 * cos(lat1) * cos(lon1)
        val y1 = r1 * cos(lat1) * sin(lon1)
        val z1 = r1 * sin(lat1)
        val x2 = r2 * cos(lat2) * cos(lon2)
        val y2 = r2 * cos(lat2) * sin(lon2)
        val z2 = r2 * sin(lat2)
        val rx = x2 - x1
        val ry = y2 - y1
        val rz = z2 - z1
        val s = -sin(lat1) * cos(lon1) * rx - sin(lat1) * sin(lon1) * ry + cos(lat1) * rz
        val e = -sin(lon1) * rx + cos(lon1) * ry
        val z = cos(lat1) * cos(lon1) * rx + cos(lat1) * sin(lon1) * ry + sin(lat1) * rz
        val range = sqrt(s * s + e * e + z * z)
        val el = if (range > 0) asin((z / range).coerceIn(-1.0, 1.0)) else 0.0
        val az = (atan2(e, s) + 2 * PI) % (2 * PI)
        return Pair(Math.toDegrees(az), Math.toDegrees(el))
    }
}

data class SatelliteMetadata(
    val id: String,
    val name: String,
    val description: String,
    val category: String = "Other",
    val commInfo: SatelliteCommInfo? = null
)
