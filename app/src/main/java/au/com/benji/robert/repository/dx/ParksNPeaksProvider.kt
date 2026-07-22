package au.com.benji.robert.repository.dx

import android.util.Log
import au.com.benji.robert.models.DxSpot
import au.com.benji.robert.models.SpotSource
import au.com.benji.robert.network.ApiService
import kotlinx.serialization.json.*

class ParksNPeaksProvider : DxSpotProvider {
    override val name: String = "ParksNPeaks"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchSpots(): List<DxSpot> {
        val spots = mutableListOf<DxSpot>()
        try {
            // Using the full URL which is more reliable
            val response = ApiService.fetchData("https://www.parksnpeaks.org/api/v1/spots")
            Log.d("ParksNPeaksProvider", "Response received: ${response?.take(100)}...")
            
            response?.let {
                val element = json.parseToJsonElement(it)
                val array = when (element) {
                    is JsonArray -> element
                    is JsonObject -> element["spots"]?.jsonArray ?: element["data"]?.jsonArray ?: JsonArray(emptyList())
                    else -> JsonArray(emptyList())
                }
                
                Log.d("ParksNPeaksProvider", "Parsed ${array.size} potential spots")
                array.forEach { item ->
                    try {
                        val obj = item.jsonObject
                        val comment = obj["comments"]?.jsonPrimitive?.content 
                            ?: obj["comment"]?.jsonPrimitive?.content 
                            ?: obj["info"]?.jsonPrimitive?.content ?: ""
                            
                        val activator = obj["activator"]?.jsonPrimitive?.content 
                            ?: obj["activatorCallsign"]?.jsonPrimitive?.content 
                            ?: obj["dx_call"]?.jsonPrimitive?.content ?: ""
                        
                        if (activator.isNotEmpty()) {
                            // Categorize based on comment or reference
                            val ref = (obj["reference"]?.jsonPrimitive?.content 
                                ?: obj["ref"]?.jsonPrimitive?.content ?: "").uppercase()
                                
                            val source = when {
                                comment.contains("SOTA", ignoreCase = true) || ref.contains("/") && !ref.contains("-") -> SpotSource.SOTA
                                comment.contains("SIOTA", ignoreCase = true) || comment.contains("SILO", ignoreCase = true) || ref.startsWith("S-") -> SpotSource.SIOTA
                                comment.contains("WWFF", ignoreCase = true) || comment.contains("VKFF", ignoreCase = true) || ref.contains("FF-") -> SpotSource.WWFF
                                comment.contains("POTA", ignoreCase = true) || ref.contains("-") && ref.length >= 4 -> SpotSource.POTA
                                else -> SpotSource.PARKSNPEAKS
                            }
                            
                            val freq = obj["frequency"]?.jsonPrimitive?.content 
                                ?: obj["freq"]?.jsonPrimitive?.content 
                                ?: obj["frequency_mhz"]?.jsonPrimitive?.content ?: ""
                                
                            spots.add(DxSpot(
                                provider = source,
                                activator = activator,
                                callsign = activator,
                                reference = ref,
                                name = obj["name"]?.jsonPrimitive?.content ?: obj["park_name"]?.jsonPrimitive?.content ?: "",
                                frequency = freq,
                                mode = obj["mode"]?.jsonPrimitive?.content ?: "",
                                spotter = obj["spotter"]?.jsonPrimitive?.content 
                                    ?: obj["spotterCallsign"]?.jsonPrimitive?.content 
                                    ?: obj["de_call"]?.jsonPrimitive?.content ?: "",
                                timestamp = DxUtils.parseIsoToTimestamp(
                                    obj["time"]?.jsonPrimitive?.content 
                                    ?: obj["timeStamp"]?.jsonPrimitive?.content 
                                    ?: obj["spot_time"]?.jsonPrimitive?.content ?: ""
                                ),
                                comment = comment
                            ))
                        }
                    } catch (e: Exception) { /* skip bad item */ }
                }
            }
        } catch (e: Exception) {
            Log.e("ParksNPeaksProvider", "Error fetching ParksNPeaks spots: ${e.message}")
        }
        return spots
    }
}
