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
    
    // NORAD IDs - wheretheiss.at currently only supports ISS (25544)
    private val satelliteIds = listOf("25544")

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
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching position for $id: ${e.message}")
                }
                delay(1000) // Respect rate limit
            }
            if (positions.isNotEmpty()) emit(positions)
            delay(10000)
        }
    }

    fun getSatellitePasses(lat: Double, lon: Double): Flow<List<SatellitePass>> = flow {
        while (true) {
            val allPasses = mutableListOf<SatellitePass>()
            for (id in satelliteIds) {
                try {
                    // Fetch passes for the next 10 days to ensure we get results
                    val url = "https://api.wheretheiss.at/v1/satellites/$id/passes?latitude=$lat&longitude=$lon&days=10"
                    Log.d(TAG, "Fetching passes from: $url")
                    val response = ApiService.fetchData(url)
                    
                    response?.let {
                        val list = json.decodeFromString<List<JsonElement>>(it)
                        val name = getSatelliteName(id)
                        Log.d(TAG, "Found ${list.size} passes for $name")
                        
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
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching passes for $id: ${e.message}")
                }
                delay(1100) // Rate limit
            }
            
            val sortedPasses = allPasses.sortedBy { it.startTime }
            emit(sortedPasses)
            
            // Refresh passes every hour
            delay(3600 * 1000)
        }
    }
    
    private fun getSatelliteName(id: String): String {
        return when (id) {
            "25544" -> "ISS"
            else -> "SAT-$id"
        }
    }
}

data class SatellitePass(
    val name: String,
    val startTime: Long,
    val duration: Long
)
