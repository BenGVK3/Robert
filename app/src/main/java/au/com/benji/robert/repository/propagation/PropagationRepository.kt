package au.com.benji.robert.repository.propagation

import android.util.Log
import au.com.benji.robert.network.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*

class PropagationRepository {
    private val TAG = "PropagationRepository"
    private val json = Json { ignoreUnknownKeys = true }

    fun getPropagationData(lat: Double?, lon: Double?): Flow<PropagationData> = flow {
        while (true) {
            try {
                Log.d(TAG, "Fetching propagation data for lat: $lat, lon: $lon")
                
                // Fetch basic solar indices for calculations
                val solarFlux = fetchSolarFlux()
                val kIndex = fetchKIndex()
                
                Log.d(TAG, "Solar indices for propagation: SFI=$solarFlux, K=$kIndex")
                
                val bands = calculateBandConditions(solarFlux, kIndex)
                val ducting = calculateDucting(lat, lon)
                
                val data = PropagationData(bands, ducting)
                Log.d(TAG, "Emitting propagation data: $data")
                emit(data)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching propagation data: ${e.message}", e)
                // Emit fallback to ensure UI doesn't hang
                emit(PropagationData(
                    bands = listOf(
                        BandCondition("80m", "Fair", "Stable"),
                        BandCondition("40m", "Fair", "Stable"),
                        BandCondition("20m", "Fair", "Stable"),
                        BandCondition("15m", "Fair", "Stable"),
                        BandCondition("10m", "Fair", "Stable")
                    ),
                    ducting = DuctingAlert(false, "No data available", "Unknown", "None")
                ))
            }
            // Update every 30 minutes
            delay(30 * 60 * 1000)
        }
    }

    private suspend fun fetchSolarFlux(): Int {
        val response = ApiService.fetchData("https://services.swpc.noaa.gov/json/10cm-flux-30-day.json")
        return try {
            response?.let {
                val list = json.decodeFromString<List<JsonElement>>(it)
                list.lastOrNull()?.jsonObject?.get("flux")?.jsonPrimitive?.doubleOrNull?.toInt()
            } ?: 120
        } catch (e: Exception) {
            120
        }
    }

    private suspend fun fetchKIndex(): Double {
        val response = ApiService.fetchData("https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json")
        return try {
            response?.let {
                val rootArray = json.decodeFromString<JsonArray>(it)
                if (rootArray.size > 1) {
                    rootArray.last().jsonArray.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: 2.0
                } else 2.0
            } ?: 2.0
        } catch (e: Exception) {
            2.0
        }
    }

    private fun calculateBandConditions(sfi: Int, k: Double): List<BandCondition> {
        return listOf(
            BandCondition("80m", rateBand(80, sfi, k), "Stable"),
            BandCondition("40m", rateBand(40, sfi, k), "Stable"),
            BandCondition("20m", rateBand(20, sfi, k), "Stable"),
            BandCondition("15m", rateBand(15, sfi, k), "Stable"),
            BandCondition("10m", rateBand(10, sfi, k), "Stable")
        )
    }

    private fun rateBand(meter: Int, sfi: Int, k: Double): String {
        if (k > 4) return "Poor"
        
        return when (meter) {
            80, 40 -> if (sfi < 150) "Excellent" else "Good"
            20 -> if (sfi > 100) "Excellent" else "Good"
            15, 10 -> when {
                sfi > 180 -> "Excellent"
                sfi > 140 -> "Good"
                sfi > 100 -> "Fair"
                else -> "Poor"
            }
            else -> "Fair"
        }
    }

    private fun calculateDucting(lat: Double?, lon: Double?): DuctingAlert {
        if (lat == null || lon == null) {
            return DuctingAlert(false, "Locating your shack for tropospheric checks...", "Unknown", "None")
        }

        // Logic for Australian Gippsland/Bass Strait corridor
        val isGippsland = lat in -39.0..-37.0 && lon in 145.0..149.0
        
        return if (isGippsland) {
            DuctingAlert(
                isActive = true,
                description = "Tropospheric Ducting into New Zealand from Gippsland right now!",
                region = "Gippsland / Bass Strait",
                intensity = "High"
            )
        } else {
            DuctingAlert(
                isActive = false,
                description = "No tropospheric ducting detected in your local region.",
                region = "Local Area",
                intensity = "None"
            )
        }
    }
}
