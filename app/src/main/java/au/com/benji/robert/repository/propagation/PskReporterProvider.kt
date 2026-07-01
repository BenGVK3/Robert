package au.com.benji.robert.repository.propagation

import android.util.Log
import au.com.benji.robert.network.ApiService
import au.com.benji.robert.utils.calculateBearing
import au.com.benji.robert.utils.calculateDistance
import au.com.benji.robert.utils.maidenheadToLatLng
import java.util.UUID

class PskReporterProvider : PropagationProvider {
    override val name: String = "PSK Reporter"

    override suspend fun fetchSpots(
        band: String,
        mode: String,
        timeWindowMinutes: Int
    ): List<PropagationSpot> {
        val seconds = timeWindowMinutes * 60
        // Use a high limit to get more data
        val url = "https://retrieve.pskreporter.info/query?band=$band&mode=$mode&flowStartSeconds=-$seconds&limit=1000"
        
        Log.d("PskReporterProvider", "Fetching data from: $url")
        val xml = ApiService.fetchData(url)
        
        if (xml == null) {
            Log.e("PskReporterProvider", "Failed to download data from PSK Reporter")
            return emptyList()
        }
        
        Log.d("PskReporterProvider", "Downloaded XML length: ${xml.length} bytes")
        val spots = parseXml(xml, band, mode)
        
        Log.i("PskReporterProvider", "Validation: Spots downloaded: ${spots.size} (Limit was 1000)")
        return spots
    }

    private fun parseXml(xml: String, band: String, mode: String): List<PropagationSpot> {
        val spots = mutableListOf<PropagationSpot>()
        var discardedCount = 0
        var totalParsed = 0
        
        try {
            // PSK Reporter XML has <receptionReport ... /> tags
            val regex = Regex("<receptionReport\\s+([^>]+)/>")
            val matches = regex.findAll(xml)
            
            for (match in matches) {
                totalParsed++
                val attributes = match.groupValues[1]
                val attrMap = parseAttributes(attributes)
                
                val senderCallsign = attrMap["senderCallsign"] ?: continue
                val receiverCallsign = attrMap["receiverCallsign"] ?: continue
                
                // Prioritize lat/lon from attributes if present, otherwise use locator
                val sLoc = attrMap["senderLocator"]
                val rLoc = attrMap["receiverLocator"]
                
                var slat = attrMap["senderDXCCLatitude"]?.toDoubleOrNull() 
                    ?: attrMap["slat"]?.toDoubleOrNull()
                var slon = attrMap["senderDXCCLongitude"]?.toDoubleOrNull()
                    ?: attrMap["slon"]?.toDoubleOrNull()
                    
                if (slat == null || slon == null) {
                    sLoc?.let {
                        val coords = maidenheadToLatLng(it)
                        slat = coords?.first
                        slon = coords?.second
                    }
                }
                
                var rlat = attrMap["receiverDXCCLatitude"]?.toDoubleOrNull()
                    ?: attrMap["rlat"]?.toDoubleOrNull()
                var rlon = attrMap["receiverDXCCLongitude"]?.toDoubleOrNull()
                    ?: attrMap["rlon"]?.toDoubleOrNull()

                if (rlat == null || rlon == null) {
                    rLoc?.let {
                        val coords = maidenheadToLatLng(it)
                        rlat = coords?.first
                        rlon = coords?.second
                    }
                }

                if (slat == null || slon == null || rlat == null || rlon == null) {
                    discardedCount++
                    continue
                }
                
                val timestamp = attrMap["flowStartSeconds"]?.toLongOrNull()?.times(1000) ?: System.currentTimeMillis()
                val distance = attrMap["distance"]?.toDoubleOrNull() ?: calculateDistance(slat!!, slon!!, rlat!!, rlon!!)
                val bearing = calculateBearing(slat!!, slon!!, rlat!!, rlon!!)
                val snr = attrMap["sNR"]?.toIntOrNull()

                spots.add(
                    PropagationSpot(
                        id = UUID.randomUUID().toString(),
                        senderCallsign = senderCallsign,
                        senderLocator = sLoc ?: "",
                        senderLat = slat!!,
                        senderLon = slon!!,
                        receiverCallsign = receiverCallsign,
                        receiverLocator = rLoc ?: "",
                        receiverLat = rlat!!,
                        receiverLon = rlon!!,
                        band = band,
                        mode = mode,
                        timestamp = timestamp,
                        distance = distance,
                        bearing = bearing,
                        snr = snr,
                        provider = name
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("PskReporterProvider", "Error parsing PSK Reporter XML", e)
        }
        
        Log.i("PskReporterProvider", "Validation Summary:")
        Log.i("PskReporterProvider", "- Total tags found: $totalParsed")
        Log.i("PskReporterProvider", "- Successfully parsed: ${spots.size}")
        Log.i("PskReporterProvider", "- Discarded: $discardedCount (missing lat/lon/locator)")

        return spots
    }

    private fun parseAttributes(attributes: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val regex = Regex("(\\w+)=\"([^\"]*)\"")
        regex.findAll(attributes).forEach {
            map[it.groupValues[1]] = it.groupValues[2]
        }
        return map
    }
}
