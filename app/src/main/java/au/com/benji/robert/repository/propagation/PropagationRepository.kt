package au.com.benji.robert.repository.propagation

import android.util.Log
import au.com.benji.robert.database.PropagationDao
import au.com.benji.robert.database.PropagationHistoryEntity
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.network.ApiService
import au.com.benji.robert.utils.PropagationCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PropagationRepository(private val propagationDao: PropagationDao) {
    private val TAG = "PropagationRepository"
    private var lastCalculatedTimestamp: Long = 0

    fun getPropagationData(
        lat: Double?,
        lon: Double?,
        solar: SolarData?,
        muf: Double,
        sunrise: String?,
        sunset: String?
    ): Flow<PropagationData> = flow {
        while (true) {
            try {
                if (solar != null) {
                    Log.d(TAG, "Calculating real HF propagation scores...")
                    
                    val calculatedBands = PropagationCalculator.calculateAllBands(
                        solarData = solar,
                        muf = muf,
                        lat = lat,
                        lon = lon,
                        sunrise = sunrise,
                        sunset = sunset
                    )

                    val now = System.currentTimeMillis()
                    
                    // Only save to history if data is fresh (every 10 mins or so)
                    if (now - lastCalculatedTimestamp > 9 * 60 * 1000) {
                        calculatedBands.forEach { bandScore ->
                            propagationDao.insert(
                                PropagationHistoryEntity(
                                    timestamp = now,
                                    band = bandScore.band,
                                    score = bandScore.score
                                )
                            )
                        }
                        lastCalculatedTimestamp = now
                        // Clean up old history (keep 24 hours)
                        propagationDao.deleteOldHistory(now - 24 * 60 * 60 * 1000)
                    }

                    val bandsWithHistory = calculatedBands.map { bandScore ->
                        val history = propagationDao.getHistoryForBand(bandScore.band, now - 24 * 60 * 60 * 1000)
                        
                        // Simple trend logic
                        val trend = if (history.size >= 2) {
                            val last = history.last().score
                            val prev = history[history.size - 2].score
                            when {
                                last > prev -> "Improving"
                                last < prev -> "Declining"
                                else -> "Stable"
                            }
                        } else "Stable"

                        BandCondition(
                            band = bandScore.band,
                            rating = bandScore.rating,
                            trend = trend,
                            score = bandScore.score,
                            color = bandScore.colorHex,
                            history = history.map { it.score }
                        )
                    }

                    val psk6m = fetchPskActivity("6m")
                    val psk10m = fetchPskActivity("10m")

                    val ducting = fetchLiveDuctingData(lat, lon)
                    val aurora = PropagationCalculator.calculateAurora(solar)
                    val eSkip = PropagationCalculator.calculateESkip(solar, muf, psk6m, psk10m)

                    emit(PropagationData(bandsWithHistory, ducting, aurora, eSkip))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in propagation engine: ${e.message}")
            }
            delay(10 * 60 * 1000) // Recalculate every 10 minutes
        }
    }

    private suspend fun fetchPskActivity(band: String): Int {
        return try {
            val response = ApiService.fetchData("https://retrieve.pskreporter.info/query?band=$band&flowStartSeconds=-3600&rronly=1")
            response?.split("<receptionReport")?.size?.minus(1)?.coerceAtLeast(0) ?: 0
        } catch (e: Exception) {
            0
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
}
