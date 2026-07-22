package au.com.benji.robert.repository.propagation

import android.util.Log
import au.com.benji.robert.database.PropagationDao
import au.com.benji.robert.database.PropagationHistoryEntity
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.network.ApiService
import au.com.benji.robert.utils.SolarCalculations
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class BandConditionsRepository(
    private val propagationDao: PropagationDao,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val TAG = "BandConditionsRepository"
    private val mutex = Mutex()
    private val refreshInterval = TimeUnit.MINUTES.toMillis(10)
    private var lastRefreshTime = 0L

    private val _propagationData = MutableStateFlow<PropagationData?>(null)
    val propagationData: StateFlow<PropagationData?> = _propagationData.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        externalScope.launch {
            loadFromCache()
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
                        rating = getRatingLabel(latestScore),
                        trend = "Stable",
                        score = latestScore,
                        color = getRatingColor(latestScore),
                        historicalData = historyEntities.map { PropagationPoint(it.timestamp, it.score) },
                        summaries = emptyList()
                    )
                }

                if (bandsWithHistory.any { it.historicalData.isNotEmpty() }) {
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
            if (_isRefreshing.value && !force) return
            _isRefreshing.value = true
            
            try {
                withContext(Dispatchers.Default) {
                    Log.d(TAG, "Executing Next-Gen Modular Propagation Refresh...")
                    
                    val pskReports = mutableMapOf<String, Int>()
                    listOf("160m", "80m", "40m", "20m", "15m", "10m", "6m").forEach { band ->
                        pskReports[band] = fetchPskActivity(band)
                    }

                    val engineInput = PropagationEngine.EngineInput(
                        solarData = solar,
                        muf = muf,
                        lat = lat,
                        lon = lon,
                        pskReports = pskReports
                    )

                    val engineOutput = PropagationEngine.calculate(engineInput)
                    val now = System.currentTimeMillis()
                    
                    // History update every 10 mins
                    if (now - lastRefreshTime > 9 * 60 * 1000) {
                        engineOutput.bands.forEach { band ->
                            propagationDao.insert(
                                PropagationHistoryEntity(
                                    timestamp = now,
                                    band = band.band,
                                    score = band.score,
                                    muf = muf,
                                    sfi = solar.solarFlux,
                                    kIndex = solar.kIndex,
                                    aIndex = solar.aIndex,
                                    solarElevation = SolarCalculations.getSolarElevation(lat, lon),
                                    confidence = engineOutput.confidence
                                )
                            )
                        }
                    }

                    val bandsWithFullData = engineOutput.bands.map { band ->
                        val historyEntities = propagationDao.getHistoryForBand(band.band, now - 24 * 60 * 60 * 1000)
                        
                        val historyPoints = historyEntities.map { 
                            PropagationPoint(it.timestamp, it.score) 
                        }
                        
                        val forecastPoints = generateForecast(band.band, band.score)

                        val trend = if (historyEntities.size >= 2) {
                            val last = historyEntities.last().score
                            val prev = historyEntities[historyEntities.size - 2].score
                            when {
                                last > prev + 5 -> "Improving"
                                last < prev - 5 -> "Declining"
                                else -> "Stable"
                            }
                        } else "Stable"

                        band.copy(
                            trend = trend,
                            historicalData = historyPoints,
                            forecastData = forecastPoints
                        )
                    }

                    _propagationData.value = engineOutput.copy(
                        bands = bandsWithFullData,
                        ducting = fetchLiveDuctingData(lat, lon),
                        timestamp = now
                    )
                    lastRefreshTime = now
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing band conditions: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun generateForecast(band: String, currentScore: Int): List<PropagationPoint> {
        val forecast = mutableListOf<PropagationPoint>()
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        for (i in 1..6) {
            val futureTime = now + (i * 60 * 60 * 1000)
            val futureHour = (calendar.get(Calendar.HOUR_OF_DAY) + i) % 24
            
            val cycleModifier = when (band) {
                "160m", "80m" -> if (futureHour in 6..18) -40 else 20
                "40m" -> if (futureHour in 7..17) -20 else 10
                "20m" -> if (futureHour in 10..16) 15 else if (futureHour in 22..23 || futureHour in 0..4) -25 else 0
                "10m", "12m", "15m" -> if (futureHour in 9..17) 30 else -70
                else -> 0
            }
            
            forecast.add(PropagationPoint(futureTime, (currentScore + cycleModifier).coerceIn(5, 100)))
        }
        return forecast
    }

    fun getHistoryFlow(band: String, timeframeHours: Int): Flow<List<PropagationHistoryEntity>> = flow {
        while (true) {
            val since = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(timeframeHours.toLong())
            emit(propagationDao.getHistoryForBand(band, since))
            delay(TimeUnit.MINUTES.toMillis(5))
        }
    }.flowOn(Dispatchers.IO)

    private fun getRatingLabel(score: Int): String = when {
        score >= 90 -> "Excellent"
        score >= 75 -> "Very Good"
        score >= 60 -> "Good"
        score >= 40 -> "Fair"
        score >= 20 -> "Poor"
        else -> "Closed"
    }

    private fun getRatingColor(score: Int): String = when {
        score >= 90 -> "#2196F3"
        score >= 75 -> "#4CAF50"
        score >= 60 -> "#CDDC39"
        score >= 40 -> "#FFEB3B"
        score >= 20 -> "#FF9800"
        else -> "#F44336"
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
        if (lat == null || lon == null) return DuctingAlert(false, "No location", "Unknown", "None")
        return DuctingAlert(false, "Scanning troposphere...", "Regional", "None")
    }
}
