package au.com.benji.robert.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.net.toUri
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.database.LogEntryEntity
import au.com.benji.robert.database.ShackEntity
import au.com.benji.robert.location.LocationService
import au.com.benji.robert.models.*
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.repository.*
import au.com.benji.robert.repository.propagation.PropagationRepository
import au.com.benji.robert.repository.propagation.PropagationData
import au.com.benji.robert.repository.shack.RadioCapabilities
import au.com.benji.robert.repository.shack.toRadioCapabilities
import au.com.benji.robert.utils.calculateMaidenhead
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DashboardRepository()
    private val solarRepository = SolarDataRepository()
    private val weatherRepository = WeatherRepository()
    private val satelliteRepository = SatelliteRepository()
    private val propagationRepository = PropagationRepository()
    private val aprsRepository = AprsRepository()
    private val dxRepository = DxRepository()
    private val locationService = LocationService(application)
    private val shackRepository = ShackRepository(DatabaseModule.shackDao(application))
    private val logRepository = LogRepository(DatabaseModule.logDao(application))

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshTrigger.emit(Unit)
            delay(1000)
            _isRefreshing.value = false
        }
    }

    val locationFlow = refreshTrigger
        .onStart { emit(Unit) }
        .map { 
            val loc = locationService.getCurrentLocation()
            if (loc != null) {
                // Round to 3 decimal places to avoid constant updates for tiny movements (~100m)
                val lat = Math.round(loc.latitude * 1000.0) / 1000.0
                val lon = Math.round(loc.longitude * 1000.0) / 1000.0
                val name = locationService.getLocationName(lat, lon)
                val maidenhead = calculateMaidenhead(lat, lon)
                Quadruple(lat, lon, name, maidenhead)
            } else {
                null
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
) : java.io.Serializable {
    override fun toString(): String = "($first, $second, $third, $fourth)"
}

    val solarData = locationFlow
        .onStart { emit(null) }
        .flatMapLatest { loc -> 
            solarRepository.getSolarData(loc?.first, loc?.second)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SolarData()
        )

    val propagationData = combine(locationFlow, solarData) { loc, solar ->
        Pair(loc, solar)
    }
    .flatMapLatest { (loc, solar) ->
        // Simple heuristic: If we have solar data, use it for calculations
        // The repository will handle the actual logic if we pass the data or if it fetches its own
        // For now, let's update the repository to accept solar data or keep its own fetch logic
        propagationRepository.getPropagationData(loc?.first, loc?.second, solar)
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val weatherData = locationFlow
        .flatMapLatest { loc ->
            if (loc != null) {
                weatherRepository.getCurrentWeather(loc.first, loc.second, loc.third)
            } else {
                weatherRepository.getCurrentWeather(-37.81, 144.96, "Melbourne (Default)")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val equipment = shackRepository.equipment()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val logs = logRepository.getAllLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val aprsPackets = locationFlow
        .flatMapLatest { loc ->
            if (loc != null) {
                aprsRepository.getRecentPackets(loc.first, loc.second)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _isRefreshingDx = MutableStateFlow(false)
    val isRefreshingDx = _isRefreshingDx.asStateFlow()

    fun refreshDxSpots() {
        viewModelScope.launch {
            _isRefreshingDx.value = true
            refreshTrigger.emit(Unit)
            delay(1000)
            _isRefreshingDx.value = false
        }
    }

    private val _dxBandFilter = MutableStateFlow<String?>(null)
    val dxBandFilter = _dxBandFilter.asStateFlow()

    private val _dxModeFilter = MutableStateFlow<String?>(null)
    val dxModeFilter = _dxModeFilter.asStateFlow()

    private val _dxContinentFilter = MutableStateFlow<String?>(null)
    val dxContinentFilter = _dxContinentFilter.asStateFlow()

    fun setDxBandFilter(band: String?) { _dxBandFilter.value = band }
    fun setDxModeFilter(mode: String?) { _dxModeFilter.value = mode }
    fun setDxContinentFilter(continent: String?) { _dxContinentFilter.value = continent }

    private val rawDxSpots = merge(
        refreshTrigger.map { Unit },
        flow { while(true) { emit(Unit); delay(60000) } }
    ).flatMapLatest {
        flow { emit(dxRepository.fetchAllSpots()) }
    }

    val dxSpots = combine(
        rawDxSpots,
        _dxBandFilter,
        _dxModeFilter,
        _dxContinentFilter
    ) { spots, band, mode, continent ->
        spots.filter { spot ->
            (band == null || spot.band == band) &&
            (mode == null || spot.normalizedMode == mode) &&
            (continent == null || spot.continent == continent)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _satelliteSearchQuery = MutableStateFlow("")
    val satelliteSearchQuery = _satelliteSearchQuery.asStateFlow()

    private val _trackedSatelliteIds = MutableStateFlow(listOf("25544", "25338", "28654", "33591", "43013"))
    val trackedSatelliteIds = _trackedSatelliteIds.asStateFlow()

    fun updateSatelliteSearchQuery(query: String) {
        _satelliteSearchQuery.value = query
    }

    fun toggleTrackedSatellite(id: String) {
        val current = _trackedSatelliteIds.value.toMutableList()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _trackedSatelliteIds.value = current
    }

    val satellitePositions = trackedSatelliteIds
        .flatMapLatest { ids -> satelliteRepository.getSatellitePositions(ids) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val satellitePasses = combine(trackedSatelliteIds, locationFlow) { ids, loc ->
        Pair(ids, loc)
    }
    .flatMapLatest { (ids, loc) ->
        if (loc != null) {
            satelliteRepository.getSatellitePasses(ids, loc.first, loc.second)
        } else {
            satelliteRepository.getSatellitePasses(ids, -33.86, 151.20)
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val nextPassTimer = flow {
        while (true) {
            val passes = satellitePasses.value
            val currentTime = System.currentTimeMillis()
            
            val activePass = passes.firstOrNull { pass ->
                val startMs = pass.startTime * 1000
                val endMs = (pass.startTime + pass.duration) * 1000
                currentTime in startMs..endMs
            }
            
            if (activePass != null) {
                emit("${activePass.name} • ACTIVE NOW")
            } else {
                val nextPass = passes.firstOrNull { it.startTime * 1000 > currentTime }
                if (nextPass != null) {
                    val diff = (nextPass.startTime * 1000) - currentTime
                    val hours = diff / 1000 / 3600
                    val mins = (diff / 1000 / 60) % 60
                    val secs = (diff / 1000) % 60
                    
                    val timeStr = when {
                        hours > 0 -> "${hours}h ${mins}m"
                        mins > 0 -> "${mins}m ${secs}s"
                        else -> "${secs}s"
                    }
                    emit("${nextPass.name} • In $timeStr")
                } else {
                    if (satellitePasses.value.isEmpty()) {
                        emit("Searching for passes...")
                    } else {
                        emit("No upcoming ISS passes")
                    }
                }
            }
            delay(1000)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Locating satellites..."
    )

    val recommendation = combine(propagationData, equipment) { propagation, gear ->
        getPersonalizedRecommendation(propagation, gear)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Analyzing conditions..."
    )

    val cards = combine(solarData, weatherData, nextPassTimer, propagationData, equipment) { solar, weather, timer, propagation, gear ->
        val baseCards = mutableListOf(
            InfoCardModel(
                type = CardType.BAND,
                icon = "📡",
                title = "Live Recommendation",
                value = getPersonalizedRecommendation(propagation, gear)
            ),
            InfoCardModel(
                type = CardType.SOLAR,
                icon = "☀️",
                title = "Solar Flux",
                value = solar?.solarFlux?.toString() ?: "---"
            ),
            InfoCardModel(
                type = CardType.SOLAR,
                icon = "🌍",
                title = "K Index",
                value = solar?.kIndex?.toString() ?: "---"
            ),
            InfoCardModel(
                type = CardType.SOLAR,
                icon = "📶",
                title = "MUF",
                value = solar?.muf ?: "---"
            ),
            InfoCardModel(
                type = CardType.WEATHER,
                icon = "🌤",
                title = "Weather",
                value = weather?.let { "${it.temperature}${it.unit} • ${it.condition}" } ?: "---"
            ),
            InfoCardModel(
                type = CardType.SATELLITE,
                icon = "🛰",
                title = "Next Pass",
                value = timer
            )
        )
        baseCards
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = repository.getDashboardCards()
    )

    private fun getPersonalizedRecommendation(data: PropagationData?, gear: List<ShackEntity>): String {
        if (data == null) return "Checking conditions..."
        
        // Find bands with "Excellent" or "Good" ratings
        val goodBands = data.bands.filter { it.rating == "Excellent" || it.rating == "Good" }
        if (goodBands.isEmpty()) return "20m is your best bet"

        // Map equipment to capabilities
        val radios = gear.mapNotNull { it.toRadioCapabilities() }
        
        // Find a radio that supports one of the good bands
        for (band in goodBands) {
            val matchingRadio = radios.firstOrNull { radio -> 
                radio.bands.contains(band.band) 
            }
            if (matchingRadio != null) {
                return "Use your ${matchingRadio.model} on ${band.band}!"
            }
        }

        // Fallback if no specific gear matches the "Excellent" bands
        return "${goodBands.first().band} is opening up!"
    }

    val quickActions = listOf(
        QuickAction("📡", "Propagation", Screen.Propagation.route),
        QuickAction("📻", "KiwiSDR", Screen.Sdr.route),
        QuickAction("📍", "APRS", Screen.Aprs.route),
        QuickAction("🛰", "Satellites", Screen.Satellites.route),
        QuickAction("🛠", "Tools", Screen.Tools.route),
        QuickAction("⚙", "Settings", Screen.Settings.route)
    )

    // Logbook Actions
    fun addLog(callsign: String, frequency: String, band: String, mode: String, notes: String = "") {
        viewModelScope.launch {
            logRepository.addLog(
                LogEntryEntity(
                    callsign = callsign.trim(),
                    frequency = frequency.trim(),
                    band = band.trim(),
                    mode = mode.trim(),
                    notes = notes.trim()
                )
            )
        }
    }

    fun deleteLog(entry: LogEntryEntity) {
        viewModelScope.launch {
            logRepository.deleteLog(entry)
        }
    }

    // Shack Actions
    fun addEquipment(
        category: String,
        manufacturer: String,
        model: String,
        nickname: String,
        serialNumber: String = "",
        notes: String = "",
        imagePath: String = ""
    ) {
        viewModelScope.launch {
            val persistentPath = if (imagePath.isNotEmpty()) {
                saveImageToInternalStorage(imagePath)
            } else {
                ""
            }
            
            shackRepository.addEquipment(
                ShackEntity(
                    category = category,
                    manufacturer = manufacturer.trim(),
                    model = model.trim(),
                    nickname = nickname.trim(),
                    serialNumber = serialNumber.trim(),
                    notes = notes.trim(),
                    imagePath = persistentPath
                )
            )
        }
    }

    private fun saveImageToInternalStorage(uriString: String): String {
        if (uriString.isEmpty()) return ""
        if (uriString.startsWith("file://") || uriString.startsWith("/")) return uriString

        return try {
            val uri = uriString.toUri()
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            val fileName = "shack_${System.currentTimeMillis()}.jpg"
            val file = java.io.File(getApplication<Application>().filesDir, fileName)
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            uriString
        }
    }

    fun deleteEquipment(item: ShackEntity) {
        viewModelScope.launch {
            shackRepository.deleteEquipment(item)
        }
    }
}
