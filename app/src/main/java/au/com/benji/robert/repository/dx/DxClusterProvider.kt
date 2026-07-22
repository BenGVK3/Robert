package au.com.benji.robert.repository.dx

import android.util.Log
import au.com.benji.robert.models.DxSpot
import au.com.benji.robert.models.SpotSource
import au.com.benji.robert.network.ApiService
import kotlinx.serialization.json.*
import java.util.Locale

class DxClusterProvider : DxSpotProvider {
    override val name: String = "DX Cluster"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchSpots(): List<DxSpot> {
        val spots = mutableListOf<DxSpot>()
        try {
            val response = ApiService.fetchData("https://www.dxsummit.fi/api/v1/spots?count=500")
            Log.d("DxClusterProvider", "Response received: ${response?.take(100)}...")
            
            response?.let {
                val element = json.parseToJsonElement(it)
                val array = when {
                    element is JsonArray -> element
                    element is JsonObject && element.containsKey("spots") -> element["spots"]?.jsonArray ?: JsonArray(emptyList())
                    else -> JsonArray(emptyList())
                }
                
                Log.d("DxClusterProvider", "Parsed ${array.size} potential spots")
                array.forEach { item ->
                    try {
                        val obj = item.jsonObject
                        val freqKhz = obj["frequency"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                        val freqMhz = freqKhz / 1000.0
                        
                        spots.add(DxSpot(
                            provider = SpotSource.DX_CLUSTER,
                            callsign = obj["dx_call"]?.jsonPrimitive?.content ?: "",
                            frequency = String.format(Locale.US, "%.3f", freqMhz),
                            mode = obj["mode"]?.jsonPrimitive?.content ?: "---",
                            spotter = obj["de_call"]?.jsonPrimitive?.content ?: "",
                            timestamp = DxUtils.parseIsoToTimestamp(obj["time"]?.jsonPrimitive?.content ?: ""), 
                            comment = obj["info"]?.jsonPrimitive?.content ?: ""
                        ))
                    } catch (e: Exception) { /* skip */ }
                }
            }
        } catch (e: Exception) {
            Log.e("DxClusterProvider", "Error fetching DX Cluster spots: ${e.message}")
        }
        return spots
    }
}
