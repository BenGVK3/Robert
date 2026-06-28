package au.com.benji.robert.repository

import android.util.Log
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.network.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.PI

class SolarDataRepository {
    private val TAG = "SolarDataRepository"
    private val json = Json { ignoreUnknownKeys = true }

    fun getSolarData(lat: Double? = null, lon: Double? = null): Flow<SolarData> = flow {
        while (true) {
            try {
                Log.d(TAG, "Starting fresh solar data fetch...")
                
                // 1. Solar Flux (10.7cm)
                val sfiValue = try {
                    val sfiJson = ApiService.fetchData("https://services.swpc.noaa.gov/json/10cm-flux-30-day.json")
                    sfiJson?.let {
                        val list = json.decodeFromString<List<JsonElement>>(it)
                        list.lastOrNull()?.jsonObject?.get("flux")?.jsonPrimitive?.doubleOrNull?.toInt()
                    } ?: 128
                } catch (e: Exception) { 128 }

                // 2. K-index & A-index
                var kValue = 2.0
                var aValue = 8
                try {
                    val summaryJson = ApiService.fetchData("https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json")
                    if (summaryJson != null) {
                        val rootArray = json.decodeFromString<JsonArray>(summaryJson)
                        if (rootArray.size > 1) {
                            val lastEntry = rootArray.last().jsonArray
                            kValue = lastEntry.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: 2.0
                            aValue = lastEntry.getOrNull(2)?.jsonPrimitive?.intOrNull ?: 8
                        }
                    }
                } catch (e: Exception) { Log.e(TAG, "K-Index parse error") }

                // 3. Sunspots
                val sunspotValue = try {
                    val sunspotJson = ApiService.fetchData("https://services.swpc.noaa.gov/json/daily-sunspot-number.json")
                    sunspotJson?.let {
                        val list = json.decodeFromString<List<JsonElement>>(it)
                        list.lastOrNull()?.jsonObject?.get("sunspot_number")?.jsonPrimitive?.intOrNull
                    } ?: 0
                } catch (e: Exception) { 0 }

                // 4. MUF estimate
                val baseMuf = (sfiValue / 4.0) + 5.0
                val latFactor = if (lat != null) cos(lat * PI / 180.0) else 1.0
                val localizedMuf = baseMuf * (0.8 + 0.4 * latFactor)
                
                val data = SolarData(
                    solarFlux = sfiValue,
                    kIndex = kValue,
                    aIndex = aValue,
                    sunspots = sunspotValue,
                    muf = String.format(Locale.US, "%.1f MHz", localizedMuf),
                    lastUpdated = System.currentTimeMillis()
                )
                emit(data)
            } catch (e: Exception) {
                Log.e(TAG, "Critical error in SolarDataRepository: ${e.message}")
                emit(SolarData(128, 2.0, 8, 0, "25.0 MHz", System.currentTimeMillis()))
            }
            delay(15 * 60 * 1000)
        }
    }
}
