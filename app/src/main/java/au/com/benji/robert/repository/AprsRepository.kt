package au.com.benji.robert.repository

import android.util.Log
import au.com.benji.robert.database.CacheDao
import au.com.benji.robert.database.AprsPacketEntity
import au.com.benji.robert.models.AprsPacket
import au.com.benji.robert.network.ApiService
import au.com.benji.robert.utils.calculateDistance
import au.com.benji.robert.utils.calculateBearing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

class AprsRepository(private val cacheDao: CacheDao) {
    private val TAG = "AprsRepository"
    private val json = Json { ignoreUnknownKeys = true }
    private val API_KEY = "228274.p5xHDl70rO5Dmjc"

    fun getRecentPackets(lat: Double, lon: Double): Flow<List<AprsPacket>> {
        val refreshFlow = flow<Unit?> {
            while (true) {
                try {
                    val stations = fetchNearbyStations(lat, lon)
                    val nearestIgate = stations.firstOrNull { it.isInfrastructure } ?: stations.firstOrNull()

                    val packets = if (nearestIgate != null) {
                        val heard = fetchHeardBy(nearestIgate.callsign, lat, lon)
                        if (heard.isNotEmpty()) heard else stations.map { it.toPacket(lat, lon) }
                    } else {
                        stations.map { it.toPacket(lat, lon) }
                    }

                    if (packets.isNotEmpty()) {
                        cacheDao.insertAprsPackets(packets.map { it.toEntity() })
                        cacheDao.cleanOldAprs(System.currentTimeMillis() - 2 * 60 * 60 * 1000) // Keep 2 hours
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching APRS data: ${e.message}")
                }
                delay(60000)
                emit(null)
            }
        }

        return merge(
            cacheDao.getAprsPackets().map { list -> list.map { it.toModel(lat, lon) } },
            refreshFlow.filter { false }.map { emptyList<AprsPacket>() }
        ).distinctUntilChanged()
    }

    private suspend fun fetchNearbyStations(lat: Double, lon: Double): List<AprsEntry> {
        val url = "https://api.aprs.fi/api/get?what=loc&lat=$lat&lon=$lon&radius=50&apikey=$API_KEY&format=json"
        val response = ApiService.fetchData(url) ?: return emptyList()
        return try {
            val root = json.parseToJsonElement(response).jsonObject
            if (root["result"]?.jsonPrimitive?.content != "ok") return emptyList()
            root["entries"]?.jsonArray?.map { it.jsonObject.toAprsEntry() } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun fetchHeardBy(callsign: String, userLat: Double, userLon: Double): List<AprsPacket> {
        val url = "https://api.aprs.fi/api/get?what=heard&name=$callsign&apikey=$API_KEY&format=json"
        val response = ApiService.fetchData(url) ?: return emptyList()
        return try {
            val root = json.parseToJsonElement(response).jsonObject
            if (root["result"]?.jsonPrimitive?.content != "ok") return emptyList()
            root["entries"]?.jsonArray?.map { it.jsonObject.toAprsPacket(userLat, userLon) } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun JsonObject.toAprsEntry() = AprsEntry(
        callsign = this["name"]?.jsonPrimitive?.content ?: "Unknown",
        lat = this["lat"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0,
        lng = this["lng"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0,
        symbol = this["symbol"]?.jsonPrimitive?.content ?: "",
        timestamp = this["lasttime"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
        isInfrastructure = this["symbol"]?.jsonPrimitive?.content?.let { s -> 
            s.contains("I") || s.contains("&") || s.contains("#")
        } ?: false
    )

    private fun JsonObject.toAprsPacket(userLat: Double, userLon: Double): AprsPacket {
        val lat = this["lat"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0
        val lng = this["lng"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0
        return AprsPacket(
            callsign = this["name"]?.jsonPrimitive?.content ?: "Unknown",
            latitude = lat,
            longitude = lng,
            symbol = this["symbol"]?.jsonPrimitive?.content ?: "",
            comment = this["comment"]?.jsonPrimitive?.content,
            timestamp = this["lasttime"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
            distance = calculateDistance(userLat, userLon, lat, lng),
            bearing = calculateBearing(userLat, userLon, lat, lng)
        )
    }

    private fun AprsEntry.toPacket(userLat: Double, userLon: Double) = AprsPacket(
        callsign = callsign,
        latitude = lat,
        longitude = lng,
        symbol = symbol,
        timestamp = timestamp,
        distance = calculateDistance(userLat, userLon, lat, lng),
        bearing = calculateBearing(userLat, userLon, lat, lng)
    )

    private fun AprsPacket.toEntity() = AprsPacketEntity(
        callsign = callsign,
        lat = latitude,
        lon = longitude,
        symbol = symbol,
        comment = comment ?: "",
        timestamp = timestamp * 1000, // aprs.fi is in seconds
        distance = distance,
        bearing = bearing
    )

    private fun AprsPacketEntity.toModel(userLat: Double, userLon: Double) = AprsPacket(
        callsign = callsign,
        latitude = lat,
        longitude = lon,
        symbol = symbol,
        comment = comment,
        timestamp = timestamp / 1000,
        distance = calculateDistance(userLat, userLon, lat, lon),
        bearing = calculateBearing(userLat, userLon, lat, lon)
    )

    private data class AprsEntry(
        val callsign: String,
        val lat: Double,
        val lng: Double,
        val symbol: String,
        val timestamp: Long,
        val isInfrastructure: Boolean
    )
}
