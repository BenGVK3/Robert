package au.com.benji.robert.repository.propagation

import android.util.Log
import au.com.benji.robert.network.ApiService
import au.com.benji.robert.utils.calculateBearing
import au.com.benji.robert.utils.calculateDistance
import au.com.benji.robert.utils.maidenheadToLatLng
import java.util.UUID

class PskReporterProvider : PropagationProvider {
    override val name: String = "PSK Reporter"

    private val bandMap = mapOf(
        "160m" to "160m",
        "80m" to "80m",
        "60m" to "60m",
        "40m" to "40m",
        "30m" to "30m",
        "20m" to "20m",
        "17m" to "17m",
        "15m" to "15m",
        "12m" to "12m",
        "10m" to "10m",
        "6m" to "6m",
        "2m" to "2m",
        "70cm" to "70cm"
    )

    override suspend fun fetchSpots(
        band: String,
        mode: String,
        timeWindowMinutes: Int
    ): List<PropagationSpot> {
        // PSK Reporter often has a delay, so a very small window might return 0 results.
        // We ensure at least a 60 minute window for reliability if requested window is small.
        val effectiveMinutes = timeWindowMinutes.coerceAtLeast(30)
        val seconds = effectiveMinutes * 60
        val queryBand = bandMap[band] ?: band
        
        // Fetch 1: Global data for the band
        val globalUrl = "https://retrieve.pskreporter.info/query?band=$queryBand&mode=$mode&flowStartSeconds=-$seconds&limit=1500"
        
        // Fetch 2: Specific data for Australian stations (VK)
        val vkUrl = "https://retrieve.pskreporter.info/query?band=$queryBand&mode=$mode&flowStartSeconds=-$seconds&limit=1000&senderDXCC=Australia"
        
        Log.d("PskReporterProvider", "Fetching Global and VK data for $band")
        
        val globalXml = ApiService.fetchData(globalUrl)
        val vkXml = ApiService.fetchData(vkUrl)
        
        val spots = mutableListOf<PropagationSpot>()
        globalXml?.let { spots.addAll(parseXml(it, band, mode)) }
        vkXml?.let { 
            val vkSpots = parseXml(it, band, mode)
            // Use a Set to avoid duplicates if global already contained some VK spots
            val existingIds = spots.map { s -> "${s.senderCallsign}-${s.receiverCallsign}-${s.timestamp}" }.toSet()
            vkSpots.forEach { s ->
                val key = "${s.senderCallsign}-${s.receiverCallsign}-${s.timestamp}"
                if (!existingIds.contains(key)) {
                    spots.add(s)
                }
            }
        }
        
        return spots
    }

    private fun parseXml(xml: String, band: String, mode: String): List<PropagationSpot> {
        val spots = mutableListOf<PropagationSpot>()
        var totalMatches = 0
        var skippedInvalid = 0
        
        try {
            val regex = Regex("<receptionReport\\s+([^>]+)/>")
            val matches = regex.findAll(xml)
            
            for (match in matches) {
                totalMatches++
                val attributes = match.groupValues[1]
                val attrMap = parseAttributes(attributes)
                
                val senderCallsign = attrMap["senderCallsign"] ?: continue
                val receiverCallsign = attrMap["receiverCallsign"] ?: continue
                
                val sLoc = attrMap["senderLocator"]
                val rLoc = attrMap["receiverLocator"]
                
                // Prioritize slat/slon if present (high precision).
                // Fallback to locator-derived coordinates (medium precision).
                // DO NOT fall back to DXCC country centers as it causes "compressed band" visuals.
                var slat = attrMap["slat"]?.toDoubleOrNull()
                var slon = attrMap["slon"]?.toDoubleOrNull()
                if (slat == null || slon == null) {
                    sLoc?.let { maidenheadToLatLng(it)?.let { c -> slat = c.first; slon = c.second } }
                }
                
                var rlat = attrMap["rlat"]?.toDoubleOrNull()
                var rlon = attrMap["rlon"]?.toDoubleOrNull()
                if (rlat == null || rlon == null) {
                    rLoc?.let { maidenheadToLatLng(it)?.let { c -> rlat = c.first; rlon = c.second } }
                }

                // Final validation
                if (slat == null || slon == null || rlat == null || rlon == null ||
                    slat !in -90.0..90.0 || slon !in -180.0..180.0 ||
                    rlat !in -90.0..90.0 || rlon !in -180.0..180.0) {
                    skippedInvalid++
                    continue
                }
                
                val timestamp = attrMap["flowStartSeconds"]?.toLongOrNull()?.times(1000) ?: System.currentTimeMillis()
                val distance = calculateDistance(slat!!, slon!!, rlat!!, rlon!!)
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
            Log.d("PskReporterProvider", "Parsed $band: Total=$totalMatches, Valid=${spots.size}, Skipped=$skippedInvalid")
        } catch (e: Exception) {
            Log.e("PskReporterProvider", "Error parsing PSK Reporter XML", e)
        }
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
