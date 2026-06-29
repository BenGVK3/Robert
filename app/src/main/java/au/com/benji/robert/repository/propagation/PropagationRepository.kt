package au.com.benji.robert.repository.propagation

import android.util.Log
import au.com.benji.robert.network.ApiService
import au.com.benji.robert.models.SolarData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*

class PropagationRepository {
    private val TAG = "PropagationRepository"
    private val json = Json { ignoreUnknownKeys = true }

    fun getPropagationData(lat: Double?, lon: Double?, solar: SolarData?): Flow<PropagationData> = flow {
        while (true) {
            try {
                Log.d(TAG, "Calculating propagation from HamQSL data...")
                
                val sfi = solar?.solarFlux ?: 120
                val kIdx = solar?.kIndex?.toDouble() ?: 2.0
                
                val xRayLevel = solar?.xRay?.take(1) ?: "A" // A, B, C, M, X
                val bands = calculateBandConditions(sfi, kIdx, xRayLevel)
                val ducting = fetchLiveDuctingData(lat, lon)
                
                emit(PropagationData(bands, ducting))
            } catch (e: Exception) {
                Log.e(TAG, "Error in propagation check: ${e.message}")
            }
            delay(15 * 60 * 1000)
        }
    }

    private suspend fun fetchLiveDuctingData(lat: Double?, lon: Double?): DuctingAlert {
        if (lat == null || lon == null) {
            return DuctingAlert(false, "Waiting for location...", "Unknown", "None")
        }

        try {
            val pskResponse = ApiService.fetchData("https://retrieve.pskreporter.info/query?band=2m&flowStartSeconds=-3600&rronly=1")
            
            if (pskResponse != null) {
                val reports = pskResponse.split("<receptionReport")
                
                val transTasmanSpots = reports.filter { report ->
                    (report.contains("senderCallsign=\"VK") && report.contains("receiverCallsign=\"ZL")) ||
                    (report.contains("senderCallsign=\"ZL") && report.contains("receiverCallsign=\"VK"))
                }

                if (transTasmanSpots.isNotEmpty()) {
                    return DuctingAlert(true, "Confirmed Tropospheric Ducting: Live signals between AU and NZ!", "Trans-Tasman", "High")
                }

                val longInternalSpots = reports.filter { report ->
                    report.contains("senderCallsign=\"VK") && report.contains("receiverCallsign=\"VK") &&
                    (report.contains("distance=\"4") || report.contains("distance=\"5") || 
                     report.contains("distance=\"6") || report.contains("distance=\"7"))
                }

                if (longInternalSpots.isNotEmpty()) {
                    return DuctingAlert(true, "Enhanced Tropo: Strong long-distance internal VHF paths detected.", "Regional AU", "Moderate")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ducting check failed: ${e.message}")
        }

        return DuctingAlert(false, "No tropospheric ducting detected in your local region.", "Local Area", "None")
    }

    private fun calculateBandConditions(sfi: Int, k: Double, xRay: String): List<BandCondition> {
        val allBands = listOf(
            "160m", "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m", "2m", "70cm"
        )
        return allBands.map { bandName ->
            val meter = bandName.replace("m", "").replace("cm", "").toIntOrNull() ?: 20
            BandCondition(bandName, rateBand(meter, sfi, k, xRay, bandName.contains("cm")), "Stable")
        }
    }

    private fun rateBand(meter: Int, sfi: Int, k: Double, xRay: String, isCm: Boolean): String {
        if ((xRay == "M" || xRay == "X") && !isCm && meter >= 6) return "Poor"
        if (k >= 5) return "Poor"
        if (k >= 4) return "Fair"

        return if (isCm || meter <= 6) {
            if (k < 2) "Good" else "Fair"
        } else {
            when {
                meter >= 40 -> if (sfi < 150 && k < 3) "Excellent" else "Good"
                meter in 17..30 -> if (sfi > 120 && k < 3) "Excellent" else "Good"
                meter in 10..15 -> when {
                    sfi > 180 && k < 2 -> "Excellent"
                    sfi > 140 && k < 3 -> "Good"
                    sfi > 100 -> "Fair"
                    else -> "Poor"
                }
                else -> "Fair"
            }
        }
    }
}
