package au.com.benji.robert.repository.lookup

import au.com.benji.robert.models.CallsignLookupResult
import au.com.benji.robert.models.ServiceCredential
import au.com.benji.robert.network.ApiService
import kotlinx.serialization.json.*

class CallookProvider : ICallsignLookupProvider {
    override val name: String = "Callook"

    override suspend fun lookup(callsign: String, credential: ServiceCredential): CallsignLookupResult? {
        // Callook is US-only, check first
        if (!isUsCallsign(callsign)) return null
        
        try {
            val jsonStr = ApiService.fetchData("https://callook.info/$callsign/json") ?: return null
            val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonStr).jsonObject
            
            if (json["status"]?.jsonPrimitive?.content == "VALID") {
                val name = json["name"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: ""
                val qth = json["address"]?.jsonObject?.get("line2")?.jsonPrimitive?.content ?: ""
                val grid = json["location"]?.jsonObject?.get("gridsquare")?.jsonPrimitive?.content ?: ""
                
                return CallsignLookupResult(
                    callsign = callsign,
                    name = name,
                    qth = qth,
                    gridsquare = grid,
                    country = "USA",
                    dxcc = "USA",
                    source = name
                )
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    override suspend fun testConnection(credential: ServiceCredential): Boolean = true // Public API

    private fun isUsCallsign(call: String): Boolean {
        val upper = call.uppercase()
        return upper.startsWith("W") || upper.startsWith("K") || upper.startsWith("N") || upper.startsWith("A")
    }
}
