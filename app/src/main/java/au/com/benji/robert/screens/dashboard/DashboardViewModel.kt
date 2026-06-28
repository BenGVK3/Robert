package au.com.benji.robert.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.database.ShackEntity
import au.com.benji.robert.location.LocationService
import au.com.benji.robert.models.CardType
import au.com.benji.robert.models.DetailedWeather
import au.com.benji.robert.models.InfoCardModel
import au.com.benji.robert.models.QuickAction
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.repository.DashboardRepository
import au.com.benji.robert.repository.SatelliteRepository
import au.com.benji.robert.repository.SolarDataRepository
import au.com.benji.robert.repository.WeatherRepository
import au.com.benji.robert.repository.propagation.PropagationRepository
import au.com.benji.robert.repository.propagation.PropagationData
import au.com.benji.robert.repository.shack.RadioCapabilities
import au.com.benji.robert.repository.shack.toRadioCapabilities
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
    private val locationService = LocationService(application)
    private val shackDao = DatabaseModule.shackDao(application)

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

    private val locationFlow = refreshTrigger
        .onStart { emit(Unit) }
        .map { 
            val loc = locationService.getCurrentLocation()
            if (loc != null) {
                val name = locationService.getLocationName(loc.latitude, loc.longitude)
                Triple(loc.latitude, loc.longitude, name)
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

    val solarData = locationFlow
        .flatMapLatest { loc -> 
            solarRepository.getSolarData(loc?.first, loc?.second)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val propagationData = locationFlow
        .flatMapLatest { loc ->
            propagationRepository.getPropagationData(loc?.first, loc?.second)
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

    private val equipment = shackDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val satellitePositions = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest { satelliteRepository.getSatellitePositions() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val satellitePasses = locationFlow
        .flatMapLatest { loc ->
            if (loc != null) {
                satelliteRepository.getSatellitePasses(loc.first, loc.second)
            } else {
                satelliteRepository.getSatellitePasses(-33.86, 151.20)
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
        QuickAction("📻", "SDR", Screen.Sdr.route),
        QuickAction("📍", "APRS", Screen.Aprs.route),
        QuickAction("🛰", "Satellites", Screen.Satellites.route),
        QuickAction("📖", "Logbook", Screen.Logbook.route),
        QuickAction("🔧", "Shack", Screen.Shack.route),
        QuickAction("🛠", "Tools", Screen.Tools.route),
        QuickAction("⚙", "Settings", Screen.Settings.route)
    )
}
