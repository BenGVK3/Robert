package au.com.benji.robert.repository.propagation

import android.util.Log
import android.util.Xml
import au.com.benji.robert.database.PropagationDao
import au.com.benji.robert.database.PropagationHistoryEntity
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.models.PskSpot
import au.com.benji.robert.network.ApiService
import au.com.benji.robert.utils.PropagationCalculator
import au.com.benji.robert.utils.maidenheadToLatLng
import au.com.benji.robert.utils.calculateDistance
import au.com.benji.robert.utils.calculateBearing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

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
                        propagationDao.deleteOldHistory(now - 24 * 60 * 60 * 1000)
                    }

                    val bandsWithHistory = calculatedBands.map { bandScore ->
                        val history = propagationDao.getHistoryForBand(bandScore.band, now - 24 * 60 * 60 * 1000)
                        
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
            delay(10 * 60 * 1000)
        }
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
