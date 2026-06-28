package au.com.benji.robert.repository

import android.util.Log
import au.com.benji.robert.network.ApiService
import au.com.benji.robert.network.SatellitePosition
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SatelliteRepository {
    private val TAG = "SatelliteRepository"
    private val json = Json { ignoreUnknownKeys = true }
    
    // NORAD IDs for popular satellites
    private val satelliteIds = listOf(
        "25544", // ISS
        "25338", // NOAA 15
        "28654", // NOAA 18
        "33591"  // NOAA 19
    )

    fun getSatellitePositions(): Flow<List<SatellitePosition>> = flow {
        while (true) {
            val positions = mutableListOf<SatellitePosition>()
            for (id in satelliteIds) {
                try {
                    val response = ApiService.fetchData("https://api.wheretheiss.at/v1/satellites/$id")
                    response?.let {
                        val pos = json.decodeFromString<SatellitePosition>(it)
                        positions.add(pos)
                    }
                    // Rate limit: The API allows 1 request per second
                    delay(1100)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching position for $id: ${e.message}")
                }
            }
            emit(positions)
            // Wait a bit before next full sweep
            delay(10000)
        }
    }

    fun getSatellitePasses(lat: Double, lon: Double): Flow<List<SatellitePass>> = flow {
        while (true) {
            val allPasses = mutableListOf<SatellitePass>()
            for (id in satelliteIds) {
                try {
                    val url = "https://api.wheretheiss.at/v1/satellites/$id/passes?latitude=$lat&longitude=$lon"
                    val response = ApiService.fetchData(url)
                    response?.let {
                        val list = json.decodeFromString<List<JsonElement>>(it)
                        val name = getSatelliteName(id)
                        list.forEach { element ->
                            val obj = element.jsonObject
                            allPasses.add(
                                SatellitePass(
                                    name = name,
                                    startTime = obj["start"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                                    duration = obj["duration"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                                )
                            )
                        }
                    }
                    delay(1100) // Rate limit
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching passes for $id: ${e.message}")
                }
            }
            // Sort passes by start time
            val sortedPasses = allPasses.sortedBy { it.startTime }
            emit(sortedPasses)
            
            // Refresh passes every hour
            delay(3600 * 1000)
        }
    }
    
    private fun getSatelliteName(id: String): String {
        return when (id) {
            "25544" -> "ISS"
            "25338" -> "NOAA 15"
            "28654" -> "NOAA 18"
            "33591" -> "NOAA 19"
            else -> "SAT-$id"
        }
    }
}

data class SatellitePass(
    val name: String,
    val startTime: Long,
    val duration: Long
)
