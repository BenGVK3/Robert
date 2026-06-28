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
                Log.d(TAG, "Starting live propagation and ducting check...")
                
                val solarFlux = fetchSolarFlux()
                val kIndex = fetchKIndex()
                val bands = calculateBandConditions(solarFlux, kIndex)
                val ducting = fetchLiveDuctingData(lat, lon)
                
                emit(PropagationData(bands, ducting))
            } catch (e: Exception) {
                Log.e(TAG, "Error in propagation check: ${e.message}")
            }
            delay(15 * 60 * 1000)
        }
    }

    private suspend fun fetchSolarFlux(): Int {
        val response = ApiService.fetchData("https://services.swpc.noaa.gov/json/10cm-flux-30-day.json")
        return try {
            response?.let {
                val list = json.decodeFromString<List<JsonElement>>(it)
                list.lastOrNull()?.jsonObject?.get("flux")?.jsonPrimitive?.doubleOrNull?.toInt()
            } ?: 120
        } catch (e: Exception) { 120 }
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
        } catch (e: Exception) { 2.0 }
    }

    private suspend fun fetchLiveDuctingData(lat: Double?, lon: Double?): DuctingAlert {
        if (lat == null || lon == null) {
            return DuctingAlert(false, "Waiting for location...", "Unknown", "None")
        }

        try {
            // Check 2m activity in the last hour
            val pskResponse = ApiService.fetchData("https://retrieve.pskreporter.info/query?band=2m&flowStartSeconds=-3600&rronly=1")
            
            if (pskResponse != null) {
                // To confirm Trans-Tasman ducting, we need a SINGLE reception report that contains BOTH VK and ZL.
                // We'll split by <receptionReport to isolate individual spots.
                val reports = pskResponse.split("<receptionReport")
                
                val transTasmanSpots = reports.filter { report ->
                    (report.contains("senderCallsign=\"VK") && report.contains("receiverCallsign=\"ZL")) ||
                    (report.contains("senderCallsign=\"ZL") && report.contains("receiverCallsign=\"VK"))
                }

                if (transTasmanSpots.isNotEmpty()) {
                    Log.d(TAG, "Confirmed Trans-Tasman ducting found in PSK Reporter data!")
                    return DuctingAlert(
                        isActive = true,
                        description = "Confirmed Tropospheric Ducting: Live signals detected between Australia and New Zealand!",
                        region = "Trans-Tasman",
                        intensity = "High"
                    )
                }

                // Check for significant internal ducting (VK to VK over long distance)
                val longInternalSpots = reports.filter { report ->
                    report.contains("senderCallsign=\"VK") && report.contains("receiverCallsign=\"VK") &&
                    // Simple heuristic: distance field starting with a large digit (indicating > 300km approx)
                    // Note: This is a rough string-based check since we are avoiding a full XML parser
                    (report.contains("distance=\"4") || report.contains("distance=\"5") || 
                     report.contains("distance=\"6") || report.contains("distance=\"7"))
                }

                if (longInternalSpots.isNotEmpty()) {
                    return DuctingAlert(
                        isActive = true,
                        description = "Enhanced Tropo: Strong internal VHF paths (>400km) detected within Australia.",
                        region = "South-East Australia",
                        intensity = "Moderate"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ducting check failed: ${e.message}")
        }

        return DuctingAlert(
            isActive = false,
            description = "No tropospheric ducting detected in your local region.",
            region = "Local Area",
            intensity = "None"
        )
    }

    private fun calculateBandConditions(sfi: Int, k: Double): List<BandCondition> {
        val allBands = listOf(
            "160m", "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m", "2m", "70cm"
        )
        return allBands.map { bandName ->
            val meter = bandName.replace("m", "").replace("cm", "").toIntOrNull() ?: 20
            BandCondition(bandName, rateBand(meter, sfi, k, bandName.contains("cm")), "Stable")
        }
    }

    private fun rateBand(meter: Int, sfi: Int, k: Double, isCm: Boolean): String {
        if (k >= 4) return "Poor"
        if (k >= 3) return "Fair"

        return if (isCm || meter <= 6) {
            if (k < 2) "Good" else "Fair"
        } else {
            when {
                meter >= 40 -> if (sfi < 150) "Excellent" else "Good"
                meter in 17..30 -> if (sfi > 120) "Excellent" else "Good"
                meter in 10..15 -> when {
                    sfi > 180 -> "Excellent"
                    sfi > 140 -> "Good"
                    sfi > 100 -> "Fair"
                    else -> "Poor"
                }
                else -> "Fair"
            }
        }
    }
}
