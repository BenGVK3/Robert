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
    
    // NORAD IDs for popular amateur and weather satellites
    val commonSatellites = mapOf(
        "25544" to "ISS",
        "25338" to "NOAA 15",
        "28654" to "NOAA 18",
        "33591" to "NOAA 19",
        "43013" to "AO-91",
        "43770" to "AO-92",
        "40069" to "XW-2A",
        "44443" to "FO-99",
        "40967" to "LilacSat-2",
        "40903" to "SaudiSat-4"
    )

    fun getSatellitePosition(id: String): Flow<SatellitePosition?> = flow {
        while (true) {
            try {
                val response = ApiService.fetchData("https://api.wheretheiss.at/v1/satellites/$id")
                response?.let {
                    val pos = json.decodeFromString<SatellitePosition>(it)
                    val name = commonSatellites[id] ?: pos.name
                    emit(pos.copy(name = name))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching position for $id: ${e.message}")
                emit(null)
            }
            delay(10000) // Refresh every 10 seconds
        }
    }

    fun getSatellitePositions(ids: List<String>): Flow<List<SatellitePosition>> = flow {
        while (true) {
            val positions = mutableListOf<SatellitePosition>()
            for (id in ids) {
                try {
                    val response = ApiService.fetchData("https://api.wheretheiss.at/v1/satellites/$id")
                    response?.let {
                        val pos = json.decodeFromString<SatellitePosition>(it)
                        val name = commonSatellites[id] ?: pos.name
                        positions.add(pos.copy(name = name))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching position for $id: ${e.message}")
                }
                delay(1000) // Respect rate limit
            }
            emit(positions)
            delay(30000) // Update all every 30 seconds
        }
    }

    fun getSatellitePasses(ids: List<String>, lat: Double, lon: Double): Flow<List<SatellitePass>> = flow {
        while (true) {
            val allPasses = mutableListOf<SatellitePass>()
            for (id in ids) {
                try {
                    val url = "https://api.wheretheiss.at/v1/satellites/$id/passes?latitude=$lat&longitude=$lon&days=10"
                    val response = ApiService.fetchData(url)
                    
                    response?.let {
                        val list = json.decodeFromString<List<JsonElement>>(it)
                        val name = commonSatellites[id] ?: "SAT-$id"
                        
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
            delay(3600 * 1000) // Refresh passes every hour
        }
    }
}

data class SatellitePass(
    val name: String,
    val startTime: Long,
    val duration: Long
)
