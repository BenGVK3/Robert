package au.com.benji.robert.repository

import au.com.benji.robert.models.SolarData
import au.com.benji.robert.network.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SolarDataRepository {
    private val json = Json { ignoreUnknownKeys = true }

    fun getSolarData(): Flow<SolarData> = flow {
        while (true) {
            try {
                val sfiJson = ApiService.fetchData("https://services.swpc.noaa.gov/json/solar-cycle/solar-flux-last-30-days.json")
                val sfiValue = sfiJson?.let {
                    val list = json.decodeFromString<List<JsonElement>>(it)
                    list.lastOrNull()?.jsonObject?.get("flux")?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt()
                } ?: 128

                val summaryJson = ApiService.fetchData("https://services.swpc.noaa.gov/products/indices.json")
                var kValue = 2.0
                var aValue = 8
                summaryJson?.let {
                    val list = json.decodeFromString<List<JsonElement>>(it)
                    val last = list.lastOrNull()?.jsonObject
                    kValue = last?.get("k_index")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 2.0
                    aValue = last?.get("a_index")?.jsonPrimitive?.content?.toIntOrNull() ?: 8
                }

                emit(
                    SolarData(
                        solarFlux = sfiValue,
                        kIndex = kValue,
                        aIndex = aValue,
                        muf = "${(sfiValue / 4) + 5} MHz"
                    )
                )
            } catch (e: Exception) {
                // Keep trying
            }
            delay(15 * 60 * 1000)
        }
    }
}
