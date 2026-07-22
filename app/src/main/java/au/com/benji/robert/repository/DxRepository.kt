package au.com.benji.robert.repository

import android.util.Log
import au.com.benji.robert.database.CacheDao
import au.com.benji.robert.database.DxSpotEntity
import au.com.benji.robert.models.DxSpot
import au.com.benji.robert.models.SpotSource
import au.com.benji.robert.repository.dx.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Locale

class DxRepository(private val cacheDao: CacheDao) {
    private val TAG = "DxRepository"
    
    private val providers = listOf(
        SotaProvider(),
        PotaProvider(),
        WwffProvider(),
        ParksNPeaksProvider(),
        DxClusterProvider(),
        RbnProvider(),
        DigitalProvider()
    )

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

    suspend fun fetchAllSpots(): List<DxSpot> = coroutineScope {
        val deferredSpots = providers.map { provider ->
            async {
                try {
                    provider.fetchSpots()
                } catch (e: Exception) {
                    Log.e(TAG, "Provider ${provider.name} failed: ${e.message}")
                    emptyList<DxSpot>()
                }
            }
        }

        val allSpots = deferredSpots.awaitAll().flatten()
        
        // Deduplication and Sorting
        // Use a more robust key: callsign + frequency + reference + provider
        allSpots.distinctBy { spot ->
            "${spot.callsign}-${spot.frequency}-${spot.reference}-${spot.provider}"
        }.sortedByDescending { it.timestamp }
    }

    private fun DxSpot.toEntity() = DxSpotEntity(
        frequency = frequency,
        callsign = callsign,
        date = "", 
        de = spotter,
        band = band, 
        mode = mode,
        comment = comment,
        timestamp = timestamp,
        source = provider.name,
        location = location,
        activator = activator,
        reference = reference,
        name = name,
        country = country,
        latitude = latitude,
        longitude = longitude,
        spotUrl = spotUrl
    )

    private fun DxSpotEntity.toModel() = DxSpot(
        callsign = callsign,
        frequency = frequency,
        mode = mode,
        spotter = de,
        timestamp = timestamp,
        comment = comment,
        provider = try { SpotSource.valueOf(source) } catch(e: Exception) { SpotSource.DX_CLUSTER },
        location = location,
        activator = activator,
        reference = reference,
        name = name,
        country = country,
        latitude = latitude,
        longitude = longitude,
        spotUrl = spotUrl
    )
}
