package au.com.benji.robert.repository

import android.content.Context
import android.util.Log
import au.com.benji.robert.database.DatabaseProvider
import au.com.benji.robert.database.RepeaterDao
import au.com.benji.robert.database.RepeaterEntity
import au.com.benji.robert.models.Repeater
import au.com.benji.robert.models.toModel
import au.com.benji.robert.utils.calculateBearing
import au.com.benji.robert.utils.calculateDistance
import au.com.benji.robert.utils.getCompassDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class RepeaterRepository(private val context: Context) {
    private val TAG = "RepeaterRepository"
    private val repeaterDao: RepeaterDao = DatabaseProvider.getDatabase(context).repeaterDao()
    private val client = OkHttpClient.Builder().build()
    
    // Official WIA Repeater Directory CSV URL
    private val WIA_CSV_URL = "https://www.wia.org.au/members/repeaters/data/documents/Repeater%20Directory%20250925.csv"

    fun getFavorites(): Flow<List<Repeater>> = repeaterDao.getFavorites().map { entities ->
        entities.map { it.toModel() }
    }

    fun getRecent(): Flow<List<Repeater>> = repeaterDao.getRecent().map { entities ->
        entities.map { it.toModel() }
    }

    fun getAllRepeaters(): Flow<List<Repeater>> = repeaterDao.getAll().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun refreshDatabase() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Refreshing repeater database from $WIA_CSV_URL")
            val request = Request.Builder()
                .url(WIA_CSV_URL)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Failed to download CSV: ${response.code}")
                
                val inputStream = response.body?.byteStream() ?: throw Exception("Empty response body")
                val entities = WiaCsvParser.parse(inputStream)
                
                if (entities.isNotEmpty()) {
                    updateLocalDatabase(entities)
                } else {
                    throw Exception("No repeaters found in CSV")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing repeater database: ${e.message}")
            throw e
        }
    }

    private suspend fun updateLocalDatabase(newEntities: List<RepeaterEntity>) {
        // Preserve favorites and recent status
        val currentFavorites = repeaterDao.getFavorites().first().associateBy { it.callsign + it.frequency }
        val currentRecent = repeaterDao.getRecent().first().associateBy { it.callsign + it.frequency }
        
        val updatedEntities = newEntities.map { entity ->
            val key = entity.callsign + entity.frequency
            entity.copy(
                isFavorite = currentFavorites[key]?.isFavorite ?: false,
                isRecent = currentRecent[key]?.isRecent ?: false,
                lastViewed = currentRecent[key]?.lastViewed ?: 0L
            )
        }
        
        repeaterDao.deleteAll()
        repeaterDao.insertAll(updatedEntities)
        Log.d(TAG, "Imported ${updatedEntities.size} repeaters from WIA CSV")
    }

    suspend fun getNearbyRepeaters(
        lat: Double,
        lon: Double,
        radius: Int = 100,
        searchQuery: String = "",
        bandFilter: String = "All",
        modeFilter: String = "All",
        onlyFavorites: Boolean = false
    ): List<Repeater> = withContext(Dispatchers.IO) {
        val cached = repeaterDao.getAll().first()
        cached.map { entity ->
            val dist = calculateDistance(lat, lon, entity.lat, entity.lng)
            val bear = calculateBearing(lat, lon, entity.lat, entity.lng)
            entity.toModel(distance = dist, bearing = bear, direction = getCompassDirection(bear))
        }.filter { repeater ->
            val matchesDistance = repeater.distance <= radius
            val matchesSearch = searchQuery.isEmpty() || 
                repeater.callsign.contains(searchQuery, ignoreCase = true) ||
                repeater.town?.contains(searchQuery, ignoreCase = true) == true ||
                repeater.location?.contains(searchQuery, ignoreCase = true) == true ||
                repeater.frequency.contains(searchQuery)
            
            val matchesBand = bandFilter == "All" || repeater.band?.contains(bandFilter, ignoreCase = true) == true
            val matchesMode = modeFilter == "All" || repeater.mode?.contains(modeFilter, ignoreCase = true) == true
            val matchesFavorites = !onlyFavorites || repeater.isFavorite
            
            matchesDistance && matchesSearch && matchesBand && matchesMode && matchesFavorites
        }.sortedBy { it.distance }
    }

    suspend fun toggleFavorite(repeater: Repeater) {
        repeaterDao.setFavorite(repeater.callsign, repeater.frequency, !repeater.isFavorite)
    }

    suspend fun markAsRecent(repeater: Repeater) {
        repeaterDao.addToRecent(repeater.callsign, repeater.frequency, System.currentTimeMillis())
    }

    fun getRepeater(callsign: String, frequency: String): Flow<Repeater?> = flow {
        val entity = repeaterDao.getByKeys(callsign, frequency)
        emit(entity?.toModel())
    }
}
