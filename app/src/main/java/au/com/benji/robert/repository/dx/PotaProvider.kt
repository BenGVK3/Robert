package au.com.benji.robert.repository.dx

import android.util.Log
import au.com.benji.robert.models.DxSpot
import au.com.benji.robert.models.SpotSource
import au.com.benji.robert.network.ApiService
import kotlinx.serialization.json.*

class PotaProvider : DxSpotProvider {
    override val name: String = "POTA"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchSpots(): List<DxSpot> {
        val spots = mutableListOf<DxSpot>()
        try {
            val response = ApiService.fetchData("https://api.pota.app/spot/active")
            Log.d("PotaProvider", "Response received: ${response?.take(100)}...")
            response?.let {
                val element = json.parseToJsonElement(it)
                val array = when {
                    element is JsonArray -> element
                    element is JsonObject && element.containsKey("data") -> element["data"]?.jsonArray ?: JsonArray(emptyList())
                    else -> JsonArray(emptyList())
                }
                
                Log.d("PotaProvider", "Parsed ${array.size} potential spots")
                array.forEach { item ->
                    try {
                        val obj = item.jsonObject
                        // Pota API uses 'activator' or 'callsign'
                        val activator = obj["activator"]?.jsonPrimitive?.content 
                            ?: obj["callsign"]?.jsonPrimitive?.content ?: ""
                            
                        if (activator.isNotEmpty()) {
                            spots.add(DxSpot(
                                provider = SpotSource.POTA,
                                activator = activator,
                                callsign = activator,
                                reference = obj["reference"]?.jsonPrimitive?.content ?: "",
                                name = obj["name"]?.jsonPrimitive?.content ?: "",
                                frequency = obj["frequency"]?.jsonPrimitive?.content ?: "",
                                mode = obj["mode"]?.jsonPrimitive?.content ?: "",
                                spotter = obj["spotter"]?.jsonPrimitive?.content ?: "",
                                timestamp = DxUtils.parseIsoToTimestamp(obj["spotTime"]?.jsonPrimitive?.content ?: obj["timestamp"]?.jsonPrimitive?.content ?: ""),
                                comment = obj["comments"]?.jsonPrimitive?.content ?: obj["comment"]?.jsonPrimitive?.content ?: "",
                                location = obj["location"]?.jsonPrimitive?.content ?: ""
                            ))
                        }
                    } catch (e: Exception) { /* skip bad item */ }
                }
            }
        } catch (e: Exception) {
            Log.e("PotaProvider", "Error fetching POTA spots: ${e.message}")
        }
        return spots
    }
}
