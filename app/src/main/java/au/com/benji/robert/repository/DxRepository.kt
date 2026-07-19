package au.com.benji.robert.repository

import android.util.Log
import au.com.benji.robert.database.CacheDao
import au.com.benji.robert.database.DxSpotEntity
import au.com.benji.robert.models.DxSpot
import au.com.benji.robert.models.SpotSource
import au.com.benji.robert.network.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class DxRepository(private val cacheDao: CacheDao) {
    private val TAG = "DxRepository"
    private val json = Json { ignoreUnknownKeys = true }

    fun getDxSpotsFlow(): Flow<List<DxSpot>> {
        val refreshFlow = flow<Unit?> {
            while (true) {
                try {
                    val allSpots = fetchAllSpots()
                    if (allSpots.isNotEmpty()) {
                        cacheDao.insertDxSpots(allSpots.map { it.toEntity() })
                        cacheDao.cleanOldDxSpots(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
                    }
                    delay(60000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in DxRepository refresh: ${e.message}")
                    delay(60000)
                }
                emit(null)
            }
        }

        return merge(
            cacheDao.getDxSpots().map { list -> list.map { it.toModel() } },
            refreshFlow.filter { false }.map { emptyList<DxSpot>() }
        ).distinctUntilChanged()
    }

    suspend fun fetchAllSpots(): List<DxSpot> {
        val allSpots = mutableListOf<DxSpot>()

        // 1. Fetch POTA Spots
        try {
            val potaJson = ApiService.fetchData("https://api.pota.app/spot/active")
            potaJson?.let {
                val array = json.decodeFromString<JsonArray>(it)
                array.forEach { element ->
                    val obj = element.jsonObject
                    val timeStr = obj["spotTime"]?.jsonPrimitive?.content ?: ""
                    allSpots.add(DxSpot(
                        callsign = obj["activator"]?.jsonPrimitive?.content ?: "",
                        frequency = obj["frequency"]?.jsonPrimitive?.content ?: "",
                        mode = obj["mode"]?.jsonPrimitive?.content ?: "",
                        spotter = obj["spotter"]?.jsonPrimitive?.content ?: "",
                        timestamp = parseIsoToTimestamp(timeStr),
                        comment = obj["comments"]?.jsonPrimitive?.content ?: "",
                        source = SpotSource.POTA,
                        location = obj["reference"]?.jsonPrimitive?.content ?: ""
                    ))
                }
            }
        } catch (e: Exception) { Log.e(TAG, "DxRepository POTA Error: ${e.message}") }

        // 2. Fetch SOTA Spots
        try {
            val sotaJson = ApiService.fetchData("https://api2.sota.org.uk/api/spots/50")
            sotaJson?.let {
                val array = json.decodeFromString<JsonArray>(it)
                array.forEach { element ->
                    val obj = element.jsonObject
                    val timeStr = obj["timeStamp"]?.jsonPrimitive?.content ?: ""
                    allSpots.add(DxSpot(
                        callsign = obj["activatorCallsign"]?.jsonPrimitive?.content ?: "",
                        frequency = obj["frequency"]?.jsonPrimitive?.content ?: "",
                        mode = obj["mode"]?.jsonPrimitive?.content ?: "",
                        spotter = obj["spotterCallsign"]?.jsonPrimitive?.content ?: "",
                        timestamp = parseIsoToTimestamp(timeStr),
                        comment = obj["comments"]?.jsonPrimitive?.content ?: "",
                        source = SpotSource.SOTA,
                        location = obj["associationName"]?.jsonPrimitive?.content ?: ""
                    ))
                }
            }
        } catch (e: Exception) { Log.e(TAG, "DxRepository SOTA Error: ${e.message}") }

        // 3. Fetch General DX Spots (DX Summit)
        try {
            val dxJson = ApiService.fetchData("https://www.dxsummit.fi/api/v1/spots?count=100")
            dxJson?.let {
                val array = json.decodeFromString<JsonArray>(it)
                array.forEach { element ->
                    val obj = element.jsonObject
                    val freqKhz = obj["frequency"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val freqMhz = freqKhz / 1000.0
                    val timeStr = obj["time"]?.jsonPrimitive?.content ?: ""
                    
                    allSpots.add(DxSpot(
                        callsign = obj["dx_call"]?.jsonPrimitive?.content ?: "",
                        frequency = String.format(Locale.US, "%.3f", freqMhz),
                        mode = obj["mode"]?.jsonPrimitive?.content ?: "---",
                        spotter = obj["de_call"]?.jsonPrimitive?.content ?: "",
                        timestamp = parseIsoToTimestamp(timeStr), 
                        comment = obj["info"]?.jsonPrimitive?.content ?: "",
                        source = SpotSource.DX_CLUSTER
                    ))
                }
            }
        } catch (e: Exception) { Log.e(TAG, "DxRepository DX Summit Error: ${e.message}") }

        return allSpots.sortedByDescending { it.timestamp }
    }

    private fun parseIsoToTimestamp(isoString: String): Long {
        if (isoString.isEmpty()) return System.currentTimeMillis()
        return try {
            val cleaned = if (isoString.contains(" ") && !isoString.contains("T")) {
                isoString.replace(" ", "T")
            } else isoString
            val finalString = if (!cleaned.contains("+") && !cleaned.endsWith("Z")) {
                cleaned + "Z"
            } else cleaned
            ZonedDateTime.parse(finalString).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            System.currentTimeMillis()
        }
    }

    private fun DxSpot.toEntity() = DxSpotEntity(
        frequency = frequency,
        callsign = callsign,
        date = "", // Not strictly used for display if we have timestamp
        de = spotter,
        band = "", // Calculated if needed
        mode = mode,
        comment = comment,
        timestamp = timestamp
    )

    private fun DxSpotEntity.toModel() = DxSpot(
        callsign = callsign,
        frequency = frequency,
        mode = mode,
        spotter = de,
        timestamp = timestamp,
        comment = comment,
        source = SpotSource.DX_CLUSTER, // Fallback source
        location = ""
    )
}
