package au.com.benji.robert.repository.dx

import android.util.Log
import au.com.benji.robert.models.DxSpot
import au.com.benji.robert.models.SpotSource
import au.com.benji.robert.network.ApiService
import kotlinx.serialization.json.*

class WwffProvider : DxSpotProvider {
    override val name: String = "WWFF"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchSpots(): List<DxSpot> {
        val spots = mutableListOf<DxSpot>()
        try {
            // Using a more reliable endpoint if the previous one was problematic
            // The previous URL was https://wwff.co/api/spots/
            val response = ApiService.fetchData("https://wwff.co/api/spots/")
            Log.d("WwffProvider", "Response received: ${response?.take(100)}...")
            
            response?.let {
                val element = json.parseToJsonElement(it)
                // WWFF API often returns a top-level array or an object with a 'spots' key
                val array = when (element) {
                    is JsonArray -> element
                    is JsonObject -> element["spots"]?.jsonArray ?: JsonArray(emptyList())
                    else -> JsonArray(emptyList())
                }
                
                Log.d("WwffProvider", "Parsed ${array.size} potential spots")
                array.forEach { item ->
                    try {
                        val obj = item.jsonObject
                        val callsign = obj["activator"]?.jsonPrimitive?.content 
                            ?: obj["callsign"]?.jsonPrimitive?.content 
                            ?: obj["dx_call"]?.jsonPrimitive?.content ?: ""
                            
                        if (callsign.isNotEmpty()) {
                            val freq = obj["frequency"]?.jsonPrimitive?.content 
                                ?: obj["freq"]?.jsonPrimitive?.content ?: ""
                                
                            spots.add(DxSpot(
                                provider = SpotSource.WWFF,
                                activator = callsign,
                                callsign = callsign,
                                reference = obj["reference"]?.jsonPrimitive?.content 
                                    ?: obj["wwff"]?.jsonPrimitive?.content 
                                    ?: obj["ref"]?.jsonPrimitive?.content ?: "",
                                name = obj["name"]?.jsonPrimitive?.content ?: "",
                                frequency = freq,
                                mode = obj["mode"]?.jsonPrimitive?.content ?: "",
                                spotter = obj["spotter"]?.jsonPrimitive?.content 
                                    ?: obj["de_call"]?.jsonPrimitive?.content ?: "",
                                timestamp = DxUtils.parseIsoToTimestamp(
                                    obj["time"]?.jsonPrimitive?.content 
                                    ?: obj["timestamp"]?.jsonPrimitive?.content 
                                    ?: obj["spot_time"]?.jsonPrimitive?.content ?: ""
                                ),
                                comment = obj["comment"]?.jsonPrimitive?.content ?: ""
                            ))
                        }
                    } catch (e: Exception) { /* skip */ }
                }
            }
        } catch (e: Exception) {
            Log.e("WwffProvider", "Error fetching WWFF spots: ${e.message}")
        }
        return spots
    }
}
