package au.com.benji.robert.repository.dx

import android.util.Log
import au.com.benji.robert.models.DxSpot
import au.com.benji.robert.models.SpotSource
import au.com.benji.robert.network.ApiService
import kotlinx.serialization.json.*

class SotaProvider : DxSpotProvider {
    override val name: String = "SOTA"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchSpots(): List<DxSpot> {
        val spots = mutableListOf<DxSpot>()
        try {
            val response = ApiService.fetchData("https://api2.sota.org.uk/api/spots/100")
            Log.d("SotaProvider", "Response received: ${response?.take(100)}...")
            
            response?.let {
                val element = json.parseToJsonElement(it)
                val array = if (element is JsonArray) element else JsonArray(emptyList())
                
                Log.d("SotaProvider", "Parsed ${array.size} potential spots")
                array.forEach { item ->
                    try {
                        val obj = item.jsonObject
                        val callsign = obj["activatorCallsign"]?.jsonPrimitive?.content ?: ""
                        if (callsign.isNotEmpty()) {
                            spots.add(DxSpot(
                                provider = SpotSource.SOTA,
                                activator = callsign,
                                callsign = callsign,
                                reference = obj["summitCode"]?.jsonPrimitive?.content ?: "",
                                name = obj["associationName"]?.jsonPrimitive?.content ?: "",
                                frequency = obj["frequency"]?.jsonPrimitive?.content ?: "",
                                mode = obj["mode"]?.jsonPrimitive?.content ?: "",
                                spotter = obj["spotterCallsign"]?.jsonPrimitive?.content ?: "",
                                timestamp = DxUtils.parseIsoToTimestamp(obj["timeStamp"]?.jsonPrimitive?.content ?: ""),
                                comment = obj["comments"]?.jsonPrimitive?.content ?: ""
                            ))
                        }
                    } catch (e: Exception) { /* skip */ }
                }
            }
        } catch (e: Exception) {
            Log.e("SotaProvider", "Error fetching SOTA spots: ${e.message}")
        }
        return spots
    }
}
