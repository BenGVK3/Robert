package au.com.benji.robert.repository

import android.util.Log
import au.com.benji.robert.models.AprsPacket
import au.com.benji.robert.network.ApiService
import au.com.benji.robert.utils.calculateDistance
import au.com.benji.robert.utils.calculateBearing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlin.math.*

class AprsRepository {
    private val TAG = "AprsRepository"
    private val json = Json { ignoreUnknownKeys = true }
    
    // Placeholder API Key - User should replace this with their own from aprs.fi
    private val API_KEY = "228274.p5xHDl70rO5Dmjc"

    fun getRecentPackets(lat: Double, lon: Double): Flow<List<AprsPacket>> = flow {
        Log.d(TAG, "Starting APRS packet flow for $lat, $lon")
        while (true) {
            try {
                // 1. Find nearest IGate/Digi
                val stations = fetchNearbyStations(lat, lon)
                Log.d(TAG, "Found ${stations.size} nearby stations")
                val nearestIgate = stations.firstOrNull { it.isInfrastructure } ?: stations.firstOrNull()

                if (nearestIgate != null) {
                    Log.d(TAG, "Fetching packets heard by ${nearestIgate.callsign}")
                    val heard = fetchHeardBy(nearestIgate.callsign, lat, lon)
                    Log.d(TAG, "Found ${heard.size} packets heard by ${nearestIgate.callsign}")
                    
                    if (heard.isNotEmpty()) {
                        emit(heard.sortedByDescending { it.timestamp })
                    } else {
                        Log.d(TAG, "No packets heard by ${nearestIgate.callsign}, falling back to nearby stations")
                        val nearby = stations.map { it.toPacket(lat, lon) }
                        emit(nearby.sortedByDescending { it.timestamp })
                    }
                } else {
                    Log.d(TAG, "No nearby IGates found, showing nearby stations instead")
                    // Fallback: just show nearby activity
                    val nearby = stations.map { it.toPacket(lat, lon) }
                    emit(nearby.sortedByDescending { it.timestamp })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching APRS data: ${e.message}", e)
            }
            kotlinx.coroutines.delay(60000) // Refresh every 60 seconds
        }
    }

    private suspend fun fetchNearbyStations(lat: Double, lon: Double): List<AprsEntry> {
        val url = "https://api.aprs.fi/api/get?what=loc&lat=$lat&lon=$lon&radius=50&apikey=$API_KEY&format=json"
        val response = ApiService.fetchData(url) ?: return emptyList()
        
        return try {
            val root = json.parseToJsonElement(response).jsonObject
            val result = root["result"]?.jsonPrimitive?.content
            if (result != "ok") {
                val error = root["description"]?.jsonPrimitive?.content ?: "Unknown error"
                Log.e(TAG, "APRS.fi API error (loc): $error")
                return emptyList()
            }
            val entries = root["entries"]?.jsonArray ?: return emptyList()
            entries.map { it.jsonObject.toAprsEntry() }
        } catch (e: Exception) {
            Log.e(TAG, "Parsing error: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchHeardBy(callsign: String, userLat: Double, userLon: Double): List<AprsPacket> {
        val url = "https://api.aprs.fi/api/get?what=heard&name=$callsign&apikey=$API_KEY&format=json"
        val response = ApiService.fetchData(url) ?: return emptyList()

        return try {
            val root = json.parseToJsonElement(response).jsonObject
            val result = root["result"]?.jsonPrimitive?.content
            if (result != "ok") {
                val error = root["description"]?.jsonPrimitive?.content ?: "Unknown error"
                Log.e(TAG, "APRS.fi API error (heard): $error")
                return emptyList()
            }
            val entries = root["entries"]?.jsonArray ?: return emptyList()
            entries.map { it.jsonObject.toAprsPacket(userLat, userLon) }
        } catch (e: Exception) {
            Log.e(TAG, "Parsing error: ${e.message}")
            emptyList()
        }
    }

    private fun JsonObject.toAprsEntry(): AprsEntry {
        return AprsEntry(
            callsign = this["name"]?.jsonPrimitive?.content ?: "Unknown",
            lat = this["lat"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0,
            lng = this["lng"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0,
            symbol = this["symbol"]?.jsonPrimitive?.content ?: "",
            timestamp = this["lasttime"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
            isInfrastructure = this["symbol"]?.jsonPrimitive?.content?.let { s -> 
                s.contains("I") || s.contains("&") || s.contains("#")
            } ?: false
        )
    }

    private fun JsonObject.toAprsPacket(userLat: Double, userLon: Double): AprsPacket {
        val lat = this["lat"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0
        val lng = this["lng"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0
        
        return AprsPacket(
            callsign = this["name"]?.jsonPrimitive?.content ?: "Unknown",
            latitude = lat,
            longitude = lng,
            symbol = this["symbol"]?.jsonPrimitive?.content ?: "",
            comment = this["comment"]?.jsonPrimitive?.content,
            speed = this["speed"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
            course = this["course"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            altitude = this["altitude"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
            timestamp = this["lasttime"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
            distance = calculateDistance(userLat, userLon, lat, lng),
            bearing = calculateBearing(userLat, userLon, lat, lng)
        )
    }

    private fun AprsEntry.toPacket(userLat: Double, userLon: Double): AprsPacket {
        return AprsPacket(
            callsign = callsign,
            latitude = lat,
            longitude = lng,
            symbol = symbol,
            timestamp = timestamp,
            distance = calculateDistance(userLat, userLon, lat, lng),
            bearing = calculateBearing(userLat, userLon, lat, lng)
        )
    }

    private data class AprsEntry(
        val callsign: String,
        val lat: Double,
        val lng: Double,
        val symbol: String,
        val timestamp: Long,
        val isInfrastructure: Boolean
    )
}
