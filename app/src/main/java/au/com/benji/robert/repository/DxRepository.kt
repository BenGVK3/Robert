package au.com.benji.robert.repository

import android.util.Log
import au.com.benji.robert.models.DxSpot
import au.com.benji.robert.models.SpotSource
import au.com.benji.robert.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*

class DxRepository {
    private val TAG = "DxRepository"
    private val json = Json { ignoreUnknownKeys = true }

    fun getDxSpots(): Flow<List<DxSpot>> = flow {
        while (true) {
            val allSpots = mutableListOf<DxSpot>()
            
            // 1. Fetch POTA Spots
            try {
                val potaJson = ApiService.fetchData("https://api.pota.app/spot/active")
                potaJson?.let {
                    val array = json.decodeFromString<JsonArray>(it)
                    array.forEach { element ->
                        val obj = element.jsonObject
                        allSpots.add(DxSpot(
                            callsign = obj["activator"]?.jsonPrimitive?.content ?: "",
                            frequency = obj["frequency"]?.jsonPrimitive?.content ?: "",
                            mode = obj["mode"]?.jsonPrimitive?.content ?: "",
                            spotter = obj["spotter"]?.jsonPrimitive?.content ?: "",
                            time = obj["spotTime"]?.jsonPrimitive?.content?.takeLast(8)?.take(5) ?: "",
                            comment = obj["comments"]?.jsonPrimitive?.content ?: "",
                            source = SpotSource.POTA,
                            location = obj["reference"]?.jsonPrimitive?.content ?: ""
                        ))
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "POTA Error: ${e.message}") }

            // 2. Fetch SOTA Spots
            try {
                val sotaJson = ApiService.fetchData("https://api2.sota.org.uk/api/spots/20")
                sotaJson?.let {
                    val array = json.decodeFromString<JsonArray>(it)
                    array.forEach { element ->
                        val obj = element.jsonObject
                        allSpots.add(DxSpot(
                            callsign = obj["activatorCallsign"]?.jsonPrimitive?.content ?: "",
                            frequency = obj["frequency"]?.jsonPrimitive?.content ?: "",
                            mode = obj["mode"]?.jsonPrimitive?.content ?: "",
                            spotter = obj["spotterCallsign"]?.jsonPrimitive?.content ?: "",
                            time = obj["timeStamp"]?.jsonPrimitive?.content?.takeLast(8)?.take(5) ?: "",
                            comment = obj["comments"]?.jsonPrimitive?.content ?: "",
                            source = SpotSource.SOTA,
                            location = obj["associationName"]?.jsonPrimitive?.content ?: ""
                        ))
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "SOTA Error: ${e.message}") }

            // 3. Fetch General DX Spots (DX Summit)
            try {
                val dxJson = ApiService.fetchData("https://www.dxsummit.fi/api/v1/spots?count=30")
                dxJson?.let {
                    val array = json.decodeFromString<JsonArray>(it)
                    array.forEach { element ->
                        val obj = element.jsonObject
                        val freqKhz = obj["frequency"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                        val freqMhz = freqKhz / 1000.0
                        allSpots.add(DxSpot(
                            callsign = obj["dx_call"]?.jsonPrimitive?.content ?: "",
                            frequency = String.format("%.3f", freqMhz),
                            mode = obj["mode"]?.jsonPrimitive?.content ?: "---",
                            spotter = obj["de_call"]?.jsonPrimitive?.content ?: "",
                            time = obj["time"]?.jsonPrimitive?.content ?: "",
                            comment = obj["info"]?.jsonPrimitive?.content ?: "",
                            source = SpotSource.DX_CLUSTER
                        ))
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "DX Summit Error: ${e.message}") }

            emit(allSpots.sortedByDescending { it.time })
            kotlinx.coroutines.delay(60000) // Refresh every minute
        }
    }
}
