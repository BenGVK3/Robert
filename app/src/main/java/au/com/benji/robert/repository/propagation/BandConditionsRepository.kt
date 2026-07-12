package au.com.benji.robert.repository.propagation

import android.util.Log
import au.com.benji.robert.database.PropagationDao
import au.com.benji.robert.database.PropagationHistoryEntity
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.network.ApiService
import au.com.benji.robert.utils.PropagationCalculator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class BandConditionsRepository(
    private val propagationDao: PropagationDao,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val TAG = "BandConditionsRepository"
    private val mutex = Mutex()
    private val refreshInterval = TimeUnit.MINUTES.toMillis(5)
    private var lastRefreshTime = 0L

    private val _propagationData = MutableStateFlow<PropagationData?>(null)
    val propagationData: StateFlow<PropagationData?> = _propagationData.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val smoothedScores = mutableMapOf<String, Double>()
    private val alpha = 0.3

    init {
        // Load initial data from cache if available
        externalScope.launch {
            loadFromCache()
        }
        
        // Auto-refresh every 5 minutes
        externalScope.launch {
            while (true) {
                if (System.currentTimeMillis() - lastRefreshTime > refreshInterval) {
                    // This will be triggered by ViewModels with proper context
                }
                delay(TimeUnit.MINUTES.toMillis(1))
            }
        }
    }

    private suspend fun loadFromCache() {
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val bands = listOf("160m", "80m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m")
                
                val bandsWithHistory = bands.map { bandName ->
                    val historyEntities = propagationDao.getHistoryForBand(bandName, now - TimeUnit.HOURS.toMillis(24))
                    val latestScore = historyEntities.lastOrNull()?.score ?: 0
                    
                    BandCondition(
                        band = bandName,
                        rating = PropagationCalculator.getRating(latestScore),
                        trend = "Stable",
                        score = latestScore,
                        color = PropagationCalculator.getColor(latestScore),
                        history = historyEntities.map { it.score },
                        summaries = emptyList()
                    )
                }

                if (bandsWithHistory.any { it.history.isNotEmpty() }) {
                    _propagationData.value = PropagationData(
                        bands = bandsWithHistory,
                        ducting = DuctingAlert(false, "Cached data", "Unknown", "None"),
                        timestamp = now
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cache: ${e.message}")
            }
        }
    }

    suspend fun refresh(
        lat: Double,
        lon: Double,
        solar: SolarData,
        muf: Double,
        sunrise: String?,
        sunset: String?,
        force: Boolean = false
    ) {
        if (!force && System.currentTimeMillis() - lastRefreshTime < refreshInterval && _propagationData.value != null) {
            return
        }

        mutex.withLock {
            if (_isRefreshing.value) return
            _isRefreshing.value = true
            
            try {
                withContext(Dispatchers.Default) {
                    Log.d(TAG, "Requesting propagation update from Engine...")
                    
                    val psk6m = fetchPskActivity("6m")
                    val psk10m = fetchPskActivity("10m")

                    val engineInput = PropagationEngine.EngineInput(
                        solarData = solar,
                        muf = muf,
                        lat = lat,
                        lon = lon,
                        sunrise = sunrise,
                        sunset = sunset,
                        psk6m = psk6m,
                        psk10m = psk10m
                    )

                    val engineOutput = PropagationEngine.calculate(engineInput)
                    val now = System.currentTimeMillis()
                    
                    // Process scores and update history
                    val calculatedBands = engineOutput.bands.map { band ->
                        val prev = smoothedScores[band.band] ?: band.score.toDouble()
                        val smoothed = (alpha * band.score) + ((1.0 - alpha) * prev)
                        smoothedScores[band.band] = smoothed
                        
                        val finalScore = smoothed.roundToInt()
                        
                        // Intelligent History Insertion: at most every 15 mins unless significant change
                        val recentHistory = propagationDao.getHistoryForBand(band.band, now - TimeUnit.MINUTES.toMillis(30))
                        val lastHistory = recentHistory.lastOrNull()
                        
                        if (lastHistory == null || Math.abs(lastHistory.score - finalScore) >= 3 || now - lastHistory.timestamp > TimeUnit.MINUTES.toMillis(15)) {
                            propagationDao.insert(
                                PropagationHistoryEntity(
                                    timestamp = now,
                                    band = band.band,
                                    score = finalScore
                                )
                            )
                        }
                        
                        band.copy(
                            score = finalScore,
                            rating = PropagationCalculator.getRating(finalScore),
                            color = PropagationCalculator.getColor(finalScore)
                        )
                    }

                    // Prune history (keep 7 days)
                    propagationDao.deleteOldHistory(now - TimeUnit.DAYS.toMillis(7))

                    val bandsWithHistory = calculatedBands.map { bandScore ->
                        val historyEntities = propagationDao.getHistoryForBand(bandScore.band, now - TimeUnit.HOURS.toMillis(24))
                        
                        val trend = if (historyEntities.size >= 3) {
                            val lastScores = historyEntities.takeLast(3).map { it.score }
                            when {
                                lastScores[2] > lastScores[0] + 1 -> "Improving"
                                lastScores[2] < lastScores[0] - 1 -> "Declining"
                                else -> "Stable"
                            }
                        } else "Stable"

                        bandScore.copy(
                            trend = trend,
                            history = historyEntities.map { it.score }
                        )
                    }

                    val newData = engineOutput.copy(
                        bands = bandsWithHistory,
                        ducting = fetchLiveDuctingData(lat, lon),
                        timestamp = now
                    )
                    
                    _propagationData.value = newData
                    lastRefreshTime = now
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing band conditions: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun fetchPskActivity(band: String): Int {
        return try {
            val response = ApiService.fetchData("https://retrieve.pskreporter.info/query?band=$band&flowStartSeconds=-3600&rronly=1")
            response?.split("<receptionReport")?.size?.minus(1)?.coerceAtLeast(0) ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun fetchLiveDuctingData(lat: Double?, lon: Double?): DuctingAlert {
        if (lat == null || lon == null) {
            return DuctingAlert(false, "Waiting for location...", "Unknown", "None")
        }

        try {
            val pskResponse = ApiService.fetchData("https://retrieve.pskreporter.info/query?band=2m&flowStartSeconds=-3600&rronly=1")
            
            if (pskResponse != null) {
                val reports = pskResponse.split("<receptionReport")
                
                val transTasmanSpots = reports.filter { report ->
                    (report.contains("senderCallsign=\"VK") && report.contains("receiverCallsign=\"ZL")) ||
                    (report.contains("senderCallsign=\"ZL") && report.contains("receiverCallsign=\"VK"))
                }

                if (transTasmanSpots.isNotEmpty()) {
                    return DuctingAlert(true, "Confirmed Tropospheric Ducting: Live signals between AU and NZ!", "Trans-Tasman", "High")
                }

                val longInternalSpots = reports.filter { report ->
                    report.contains("senderCallsign=\"VK") && report.contains("receiverCallsign=\"VK") &&
                    (report.contains("distance=\"4") || report.contains("distance=\"5") || 
                     report.contains("distance=\"6") || report.contains("distance=\"7"))
                }

                if (longInternalSpots.isNotEmpty()) {
                    return DuctingAlert(true, "Enhanced Tropo: Strong long-distance internal VHF paths detected.", "Regional AU", "Moderate")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ducting check failed: ${e.message}")
        }

        return DuctingAlert(false, "No tropospheric ducting detected in your local region.", "Local Area", "None")
    }

    fun getHistoryFlow(band: String, timeframeHours: Int): Flow<List<PropagationHistoryEntity>> = flow {
        while (true) {
            val since = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(timeframeHours.toLong())
            emit(propagationDao.getHistoryForBand(band, since))
            delay(TimeUnit.MINUTES.toMillis(1))
        }
    }.flowOn(Dispatchers.IO)
}
