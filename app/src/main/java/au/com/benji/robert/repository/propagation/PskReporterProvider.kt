package au.com.benji.robert.repository.propagation

import android.util.Log
import au.com.benji.robert.network.ApiService
import java.util.UUID

class PskReporterProvider : PropagationProvider {
    override val name: String = "PSK Reporter"

    override suspend fun fetchSpots(
        band: String,
        mode: String,
        timeWindowMinutes: Int
    ): List<PropagationSpot> {
        // Convert band to PSK Reporter format if needed
        // e.g. "20m" to "14000000" or similar, but PSK Reporter accepts "20m"
        val seconds = timeWindowMinutes * 60
        val url = "https://retrieve.pskreporter.info/query?band=$band&mode=$mode&flowStartSeconds=-$seconds"
        
        val xml = ApiService.fetchData(url) ?: return emptyList()
        
        return parseXml(xml, band, mode)
    }

    private fun parseXml(xml: String, band: String, mode: String): List<PropagationSpot> {
        val spots = mutableListOf<PropagationSpot>()
        try {
            // Very basic XML parsing for demo purposes
            // In a real app, use a proper XML pull parser or library
            val regex = Regex("<receptionReport\\s+([^>]+)/>")
            val matches = regex.findAll(xml)
            
            for (match in matches) {
                val attributes = match.groupValues[1]
                val attrMap = parseAttributes(attributes)
                
                val senderCallsign = attrMap["senderCallsign"] ?: continue
                val senderLocator = attrMap["senderLocator"] ?: ""
                val senderLat = attrMap["senderDXCCLatitude"]?.toDoubleOrNull() 
                    ?: attrMap["slat"]?.toDoubleOrNull() ?: 0.0
                val senderLon = attrMap["senderDXCCLongitude"]?.toDoubleOrNull()
                    ?: attrMap["slon"]?.toDoubleOrNull() ?: 0.0
                
                val receiverCallsign = attrMap["receiverCallsign"] ?: continue
                val receiverLocator = attrMap["receiverLocator"] ?: ""
                val receiverLat = attrMap["receiverDXCCLatitude"]?.toDoubleOrNull()
                    ?: attrMap["rlat"]?.toDoubleOrNull() ?: 0.0
                val receiverLon = attrMap["receiverDXCCLongitude"]?.toDoubleOrNull()
                    ?: attrMap["rlon"]?.toDoubleOrNull() ?: 0.0
                
                val timestamp = attrMap["flowStartSeconds"]?.toLongOrNull()?.times(1000) ?: System.currentTimeMillis()
                val distance = attrMap["distance"]?.toDoubleOrNull() ?: 0.0
                val bearing = 0.0 // Bearings are usually calculated
                val snr = attrMap["sNR"]?.toIntOrNull()

                spots.add(
                    PropagationSpot(
                        id = UUID.randomUUID().toString(),
                        senderCallsign = senderCallsign,
                        senderLocator = senderLocator,
                        senderLat = senderLat,
                        senderLon = senderLon,
                        receiverCallsign = receiverCallsign,
                        receiverLocator = receiverLocator,
                        receiverLat = receiverLat,
                        receiverLon = receiverLon,
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
