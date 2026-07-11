package au.com.benji.robert.screens.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.net.toUri
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.database.LogEntryEntity
import au.com.benji.robert.database.ShackEntity
import au.com.benji.robert.database.NetEntity
import au.com.benji.robert.location.LocationService
import au.com.benji.robert.models.*
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.network.SatellitePosition
import au.com.benji.robert.network.SatellitePass
import au.com.benji.robert.repository.*
import au.com.benji.robert.repository.propagation.PropagationRepository
import au.com.benji.robert.repository.propagation.PropagationData
import au.com.benji.robert.repository.shack.RadioCapabilities
import au.com.benji.robert.repository.shack.toRadioCapabilities
import au.com.benji.robert.utils.calculateMaidenhead
import au.com.benji.robert.utils.calculateDistance
import au.com.benji.robert.utils.MufCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val cacheDao = DatabaseModule.cacheDao(application)
    
    private val repository = DashboardRepository()
    private val solarRepository = SolarDataRepository(cacheDao)
    private val weatherRepository = WeatherRepository(DatabaseModule.weatherDao(application))
    private val propagationRepository = PropagationRepository(DatabaseModule.propagationDao(application))
    private val moonRepository = MoonRepository(DatabaseModule.moonDao(application))
    private val aprsRepository = AprsRepository(cacheDao)
    private val dxRepository = DxRepository(cacheDao)
    private val satelliteRepository = SatelliteRepository(cacheDao)
    private val locationService = LocationService(application)
    private val settingsRepository = SettingsRepository(cacheDao)
    private val shackRepository = ShackRepository(DatabaseModule.shackDao(application))
    private val logRepository = LogRepository(DatabaseModule.logDao(application))
    private val repeaterRepository = RepeaterRepository(DatabaseModule.repeaterDao(application))
    private val netRepository = NetRepository(DatabaseModule.netDao(application))

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val callsign = settingsRepository.callsign.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "VK3XYZ"
    )

    val name = settingsRepository.name.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

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
                val lat = Math.round(loc.latitude * 1000.0) / 1000.0
                val lon = Math.round(loc.longitude * 1000.0) / 1000.0
                val name = locationService.getLocationName(lat, lon)
                val maidenhead = calculateMaidenhead(lat, lon)
                Quadruple(lat, lon, name, maidenhead)
            } else {
                // Return a default if no location yet, but don't hang
                Quadruple(-37.81, 144.96, "Melbourne (Cached)", "QF22")
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Quadruple(-37.81, 144.96, "Melbourne (Initial)", "QF22")
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
        .flatMapLatest { loc -> 
            solarRepository.getSolarData(loc.first, loc.second)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SolarData()
        )

    val mufResult = combine(solarData, locationFlow) { solar, loc ->
        MufCalculator.calculateMuf(solar, loc?.first, loc?.second)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MufCalculator.MufResult(0.0, true, MufCalculator.Confidence.Low)
    )

    val weatherData = locationFlow
        .flatMapLatest { loc ->
            weatherRepository.getCurrentWeather(loc.first, loc.second, loc.third)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val propagationData = combine(
        locationFlow,
        solarData,
        mufResult,
        weatherData
    ) { loc, solar, muf, weather ->
        pkg(loc, solar, muf, weather)
    }
    .flatMapLatest { pkg ->
        propagationRepository.getPropagationData(
            lat = pkg.loc.first,
            lon = pkg.loc.second,
            solar = pkg.solar,
            muf = pkg.muf.value,
            sunrise = pkg.weather?.sunrise,
            sunset = pkg.weather?.sunset
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private data class pkg(
        val loc: Quadruple<Double, Double, String, String>,
        val solar: SolarData,
        val muf: MufCalculator.MufResult,
        val weather: DetailedWeather?
    )

    val moonData = locationFlow
        .flatMapLatest { loc ->
            moonRepository.getMoonData(loc.first, loc.second)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MoonData()
        )

    val equipment = shackRepository.equipment()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val shackSummary = equipment.map { gear ->
        val radios = gear.count { 
            it.category.equals("Radio", ignoreCase = true) || 
            it.category.equals("SDR", ignoreCase = true) ||
            it.category.contains("Rig", ignoreCase = true) ||
            it.category.contains("Transceiver", ignoreCase = true)
        }
        val antennas = gear.count { 
            it.category.equals("Antenna", ignoreCase = true) ||
            it.category.contains("Aerial", ignoreCase = true) ||
            it.category.equals("Antenna Tuner", ignoreCase = true)
        }
        mapOf(
            "Radio" to radios,
            "Antenna" to antennas,
            "Total" to gear.size,
            "Other" to (gear.size - radios - antennas).coerceAtLeast(0)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = mapOf("Radio" to 0, "Antenna" to 0, "Total" to 0, "Other" to 0)
    )

    val logs = logRepository.getAllLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val latestLog = logs.map { it.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val aprsPackets = locationFlow
        .flatMapLatest { loc ->
            aprsRepository.getRecentPackets(loc.first, loc.second)
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

    val dxSpots = dxRepository.getDxSpotsFlow()
        .combine(_dxBandFilter) { spots, band ->
            if (band == null) spots else spots.filter { it.band == band }
        }
        .combine(_dxModeFilter) { spots, mode ->
            if (mode == null) spots else spots.filter { it.mode == mode }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteRepeater = combine(locationFlow, repeaterRepository.getFavorites()) { loc, favorites ->
        if (favorites.isNotEmpty()) {
            val fav = favorites.first()
            val dist = calculateDistance(loc.first, loc.second, fav.lat, fav.lng)
            fav.copy(distance = dist)
        } else {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val nextNet = netRepository.getAllNets()
        .map { nets ->
            if (nets.isEmpty()) return@map null
            
            val now = Calendar.getInstance()
            val currentMillis = now.timeInMillis

            nets.mapNotNull { net ->
                if (net.specificDate != null) {
                    // Handle one-off events
                    if (net.specificDate > currentMillis) {
                        Pair(net, net.specificDate)
                    } else {
                        null
                    }
                } else {
                    // Handle weekly recurring nets
                    val day = net.dayOfWeek ?: return@mapNotNull null
                    val timeParts = net.time.split(":")
                    if (timeParts.size != 2) return@mapNotNull null
                    
                    val netCalendar = Calendar.getInstance()
                    netCalendar.set(Calendar.DAY_OF_WEEK, day)
                    netCalendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    netCalendar.set(Calendar.MINUTE, timeParts[1].toInt())
                    netCalendar.set(Calendar.SECOND, 0)
                    netCalendar.set(Calendar.MILLISECOND, 0)

                    if (netCalendar.timeInMillis <= currentMillis) {
                        netCalendar.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                    
                    Pair(net, netCalendar.timeInMillis)
                }
            }
            .sortedBy { it.second }
            .firstOrNull()?.first
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _satelliteSearchQuery = MutableStateFlow("")
    val satelliteSearchQuery = _satelliteSearchQuery.asStateFlow()

    private val _selectedSatelliteId = MutableStateFlow("25544")
    val selectedSatelliteId = _selectedSatelliteId.asStateFlow()

    val favoriteSatelliteIds = satelliteRepository.getFavoriteSatelliteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val availableSatellites = satelliteRepository.availableSatellites

    fun selectSatellite(id: String) {
        _selectedSatelliteId.value = id
    }

    fun toggleFavoriteSatellite(id: String) {
        viewModelScope.launch {
            satelliteRepository.toggleFavorite(id)
        }
    }

    fun updateSatelliteSearchQuery(query: String) {
        _satelliteSearchQuery.value = query
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val satellitePosition = combine(_selectedSatelliteId, locationFlow) { id, loc ->
        Pair(id, loc)
    }.flatMapLatest { (id, loc) ->
        satelliteRepository.getPosition(id, loc.first, loc.second)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val upcomingPasses = combine(_selectedSatelliteId, locationFlow) { id, loc ->
        Pair(id, loc)
    }.flatMapLatest { (id, loc) ->
        satelliteRepository.getUpcomingPasses(id, loc.first, loc.second)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allUpcomingPasses = locationFlow.flatMapLatest { loc ->
        if (loc.first == 0.0 && loc.second == 0.0) {
            Log.d("DashboardViewModel", "Waiting for valid location for satellite passes...")
            return@flatMapLatest flowOf(emptyList<SatellitePass>())
        }
        
        // Priority satellites to check first/only to speed up dashboard
        // 25544 = ISS, 25338 = NOAA 15, 33591 = NOAA 19
        val prioritySats = listOf("25544", "25338", "33591") 
        Log.d("DashboardViewModel", "Fetching passes for priority satellites at ${loc.first}, ${loc.second}")
        
        val flows = prioritySats.map { id ->
            satelliteRepository.getUpcomingPasses(id, loc.first, loc.second)
        }
        
        combine(flows) { arrays ->
            val now = System.currentTimeMillis() / 1000
            val all = arrays.flatMap { it }
                .filter { it.startTime > now }
                .sortedBy { it.startTime }
            Log.d("DashboardViewModel", "Combined found ${all.size} upcoming priority passes")
            all
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recommendation = combine(propagationData, equipment) { propagation, gear ->
        getPersonalizedRecommendation(propagation, gear)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Analyzing conditions..."
    )

    val nextPassTimer = flow {
        while (true) {
            val passesList = allUpcomingPasses.value
            val currentTime = System.currentTimeMillis() / 1000
            
            val nextPass = passesList.firstOrNull { it.startTime > currentTime }
            if (nextPass != null) {
                val diff = nextPass.startTime - currentTime
                val mins = diff / 60
                val secs = diff % 60
                emit("${nextPass.name} • In ${mins}m ${secs}s")
            } else {
                emit("No upcoming passes")
            }
            delay(1000)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Scanning sky..."
    )

    val cards = combine(solarData, weatherData, nextPassTimer, propagationData, equipment, mufResult, moonData) { args ->
        val solar = args[0] as SolarData
        val weather = args[1] as? DetailedWeather
        val timer = args[2] as String
        val propagation = args[3] as? PropagationData
        val gear = args[4] as List<ShackEntity>
        val muf = args[5] as MufCalculator.MufResult
        val moon = args[6] as MoonData

        val baseCards = mutableListOf(
            InfoCardModel(
                type = CardType.BAND,
                icon = "📡",
                title = "Live Recommendation",
                value = getPersonalizedRecommendation(propagation, gear)
            ),
            InfoCardModel(
                type = CardType.SOLAR,
                icon = "🌕",
                title = "Moon: ${moon.phaseName}",
                value = if (moon.isVisible) "EME Possible Now" else "Moon Below Horizon"
            ),
            InfoCardModel(
                type = CardType.SOLAR,
                icon = "☀️",
                title = "Solar Flux",
                value = solar.solarFlux.toString()
            ),
            InfoCardModel(
                type = CardType.SOLAR,
                icon = "🌍",
                title = "K Index",
                value = solar.kIndex.toString()
            ),
            InfoCardModel(
                type = CardType.SOLAR,
                icon = "🌍",
                title = if (muf.isEstimated) "Estimated MUF" else "Reported MUF",
                value = String.format("%.1f MHz", muf.value) + if (muf.isEstimated) " (${muf.confidence})" else ""
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
        
        val goodBands = data.bands.filter { it.rating == "Excellent" || it.rating == "Good" }
        if (goodBands.isEmpty()) return "20m is your best bet"

        val radios = gear.mapNotNull { it.toRadioCapabilities() }
        
        for (band in goodBands) {
            val matchingRadio = radios.firstOrNull { radio -> 
                radio.bands.contains(band.band) 
            }
            if (matchingRadio != null) {
                return "Use your ${matchingRadio.model} on ${band.band}!"
            }
        }

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
    fun addLog(
        callsign: String,
        name: String = "",
        qth: String = "",
        frequency: String,
        band: String,
        mode: String,
        rstSent: String = "59",
        rstReceived: String = "59",
        power: String = "",
        timestamp: Long = System.currentTimeMillis(),
        notes: String = "",
        sotaRef: String = "",
        potaRef: String = "",
        wwffRef: String = "",
        hemaRef: String = "",
        siotaRef: String = "",
        vkShireRef: String = ""
    ) {
        viewModelScope.launch {
            logRepository.addLog(
                LogEntryEntity(
                    callsign = callsign.trim(),
                    name = name.trim(),
                    qth = qth.trim(),
                    frequency = frequency.trim(),
                    band = band.trim(),
                    mode = mode.trim(),
                    rstSent = rstSent,
                    rstReceived = rstReceived,
                    power = power.trim(),
                    timestamp = timestamp,
                    notes = notes.trim(),
                    sotaRef = sotaRef.trim(),
                    potaRef = potaRef.trim(),
                    wwffRef = wwffRef.trim(),
                    hemaRef = hemaRef.trim(),
                    siotaRef = siotaRef.trim(),
                    vkShireRef = vkShireRef.trim()
                )
            )
        }
    }

    fun updateLog(entry: LogEntryEntity) {
        viewModelScope.launch {
            logRepository.updateLog(entry)
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
