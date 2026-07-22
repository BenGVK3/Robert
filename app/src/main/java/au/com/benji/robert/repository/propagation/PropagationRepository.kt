package au.com.benji.robert.repository.propagation

import android.util.Log
import android.util.Xml
import au.com.benji.robert.database.PropagationDao
import au.com.benji.robert.database.PropagationHistoryEntity
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.models.PskSpot
import au.com.benji.robert.network.ApiService
import au.com.benji.robert.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.Calendar
import kotlin.math.roundToInt

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
                if (solar != null && lat != null && lon != null) {
                    Log.d(TAG, "Executing Next-Gen Propagation Engine...")
                    
                    // 1. Fetch real-world activity
                    val pskReports = mutableMapOf<String, Int>()
                    listOf("160m", "80m", "40m", "20m", "15m", "10m", "6m").forEach { band ->
                        pskReports[band] = fetchPskActivity(band)
                    }

                    // 2. Run Modular Calculation
                    val input = PropagationEngine.EngineInput(
                        solarData = solar,
                        muf = muf,
                        lat = lat,
                        lon = lon,
                        pskReports = pskReports
                    )
                    
                    val currentData = PropagationEngine.calculate(input)

                    val now = System.currentTimeMillis()
                    
                    // 3. Save History (Every 10 mins)
                    if (now - lastCalculatedTimestamp > 9 * 60 * 1000) {
                        currentData.bands.forEach { band ->
                            propagationDao.insert(
                                PropagationHistoryEntity(
                                    timestamp = now,
                                    band = band.band,
                                    score = band.score,
                                    muf = muf,
                                    sfi = solar.solarFlux,
                                    kIndex = solar.kIndex,
                                    aIndex = solar.aIndex,
                                    solarElevation = SolarCalculations.getSolarElevation(lat, lon),
                                    confidence = currentData.confidence
                                )
                            )
                        }
                        lastCalculatedTimestamp = now
                        propagationDao.deleteOldHistory(now - 48 * 60 * 60 * 1000) // Keep 48h
                    }

                    // 4. Build Detailed Bands with Real History and Forecast
                    val finalBands = currentData.bands.map { band ->
                        val historyEntities = propagationDao.getHistoryForBand(band.band, now - 24 * 60 * 60 * 1000)
                        
                        val rawHistoryPoints = historyEntities.map { 
                            PropagationPoint(it.timestamp, it.score) 
                        }

                        // Apply smoothing to history before sending to UI
                        val smoothedHistory = PropagationSmoother.smoothHistory(band.band, rawHistoryPoints)
                        
                        val forecastPoints = generateForecast(band.band, band.score, input)

                        val trend = if (smoothedHistory.size >= 2) {
                            val last = smoothedHistory.last().score
                            val prev = smoothedHistory[smoothedHistory.size - 2].score
                            when {
                                last > prev + 5 -> "Improving"
                                last < prev - 5 -> "Declining"
                                else -> "Stable"
                            }
                        } else "Stable"

                        band.copy(
                            trend = trend,
                            historicalData = smoothedHistory,
                            forecastData = forecastPoints
                        )
                    }

                    val ducting = fetchLiveDuctingData(lat, lon)
                    emit(currentData.copy(bands = finalBands, ducting = ducting))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in propagation engine: ${e.message}")
            }
            delay(10 * 60 * 1000)
        }
    }

    private fun generateForecast(band: String, currentScore: Int, input: PropagationEngine.EngineInput): List<PropagationPoint> {
        val forecast = mutableListOf<PropagationPoint>()
        val now = System.currentTimeMillis()
        
        // Predict next 6 hours (6 points)
        for (i in 1..6) {
            val futureTime = now + (i * 60 * 60 * 1000)
            
            // Heuristic forecast: Adjust score based on solar movement
            // This is a simplified simulation of "Time of day" movement
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val targetHour = (currentHour + i) % 24
            
            // Simple cycle logic for forecasting
            val cycleModifier = when (band) {
                "160m", "80m" -> if (targetHour in 6..18) -40 else 20
                "20m" -> if (targetHour in 10..15) 15 else if (targetHour in 22..4) -20 else 0
                "10m" -> if (targetHour in 9..17) 25 else -60
                else -> 0
            }
            
            val predictedScore = (currentScore + cycleModifier).coerceIn(5, 100)
            forecast.add(PropagationPoint(futureTime, predictedScore))
        }
        return forecast
    }

    suspend fun getLiveSpots(band: String, lat: Double?, lon: Double?): List<PskSpot> = withContext(Dispatchers.IO) {
        val url = "https://retrieve.pskreporter.info/query?band=$band&flowStartSeconds=-3600&rronly=1"
        val xml = ApiService.fetchData(url) ?: return@withContext emptyList()
        
        val spots = mutableListOf<PskSpot>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "receptionReport") {
                    val call = parser.getAttributeValue(null, "senderCallsign")
                    val loc = parser.getAttributeValue(null, "senderLocator")
                    val freq = parser.getAttributeValue(null, "frequency")?.toDoubleOrNull() ?: 0.0
                    val mode = parser.getAttributeValue(null, "mode") ?: "FT8"
                    val time = parser.getAttributeValue(null, "flowStartSeconds")?.toLongOrNull() ?: 0L
                    
                    if (call != null && loc != null) {
                        val latLon = maidenheadToLatLng(loc)
                        if (latLon != null) {
                            var dist = 0.0
                            var bear = 0.0
                            if (lat != null && lon != null) {
                                dist = calculateDistance(lat, lon, latLon.first, latLon.second)
                                bear = calculateBearing(lat, lon, latLon.first, latLon.second)
                            }
                            
                            spots.add(PskSpot(
                                callsign = call,
                                grid = loc,
                                lat = latLon.first,
                                lon = latLon.second,
                                frequency = freq / 1000000.0,
                                mode = mode,
                                reportTime = time * 1000,
                                distance = dist,
                                bearing = bear
                            ))
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing PSK spots: ${e.message}")
        }
        spots
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
