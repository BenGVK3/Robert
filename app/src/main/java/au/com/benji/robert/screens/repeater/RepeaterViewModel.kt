package au.com.benji.robert.screens.repeater

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.database.ShackEntity
import au.com.benji.robert.location.LocationService
import au.com.benji.robert.models.Repeater
import au.com.benji.robert.repository.RepeaterRepository
import au.com.benji.robert.repository.ShackRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class RepeaterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RepeaterRepository(DatabaseModule.repeaterDao(application))
    private val shackRepository = ShackRepository(DatabaseModule.shackDao(application))
    private val locationService = LocationService(application)

    private val _repeaters = MutableStateFlow<List<Repeater>>(emptyList())
    val repeaters: StateFlow<List<Repeater>> = _repeaters.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation = _userLocation.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filters = MutableStateFlow(RepeaterFilters())
    val filters = _filters.asStateFlow()

    private val _uiState = MutableStateFlow<RepeaterUiState>(RepeaterUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val filteredRepeaters = combine(_repeaters, _searchQuery, _filters) { list, query, filter ->
        list.filter { repeater ->
            val matchesSearch = query.isEmpty() || 
                repeater.callsign.contains(query, ignoreCase = true) ||
                repeater.town?.contains(query, ignoreCase = true) == true ||
                repeater.location?.contains(query, ignoreCase = true) == true ||
                repeater.frequency.contains(query)
            
            val matchesBand = filter.band == "All" || 
                matchesBandFilter(repeater.band, repeater.frequency, filter.band)
                
            val matchesMode = filter.mode == "All" || 
                matchesModeFilter(repeater.mode, filter.mode)
                
            val matchesFavorite = !filter.onlyFavorites || repeater.isFavorite
            val matchesDistance = repeater.distance <= filter.maxDistance
            
            matchesSearch && matchesBand && matchesMode && matchesFavorite && matchesDistance
        }.sortedBy { it.distance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun matchesBandFilter(repeaterBand: String?, frequency: String, filterBand: String): Boolean {
        val fBand = filterBand.lowercase().trim()
        val rBand = repeaterBand?.lowercase()?.trim() ?: ""
        
        // 1. Try mapping via frequency (most reliable)
        val freqNum = frequency.toDoubleOrNull() ?: 0.0
        val mappedBand = when {
            freqNum in 28.0..29.9 -> "10m"
            freqNum in 50.0..54.0 -> "6m"
            freqNum in 144.0..148.0 -> "2m"
            freqNum in 430.0..450.0 -> "70cm"
            freqNum > 1200.0 -> "23cm"
            else -> null
        }
        if (mappedBand == fBand) return true

        // 2. Fallback to existing band string keywords
        return when (fBand) {
            "2m" -> rBand.contains("144") || rBand.contains("146") || rBand.contains("147") || rBand.contains("vhf")
            "70cm" -> rBand.contains("43") || rBand.contains("44") || rBand.contains("uhf")
            "6m" -> rBand.contains("52") || rBand.contains("53") || rBand.contains("50")
            "10m" -> rBand.contains("29") || rBand.contains("28")
            "23cm" -> rBand.contains("1200") || rBand.contains("1290") || rBand.contains("1.2")
            else -> rBand.contains(fBand)
        }
    }

    private fun matchesModeFilter(repeaterMode: String?, filterMode: String): Boolean {
        if (repeaterMode == null) return false
        val rMode = repeaterMode.lowercase().trim()
        val fMode = filterMode.lowercase().trim()
        
        return when (fMode) {
            "fm" -> rMode.contains("fm") || rMode.contains("analog") || rMode.contains("f3e") || rMode.isEmpty() || rMode.contains("voice")
            "dmr" -> rMode.contains("dmr") || rMode.contains("mototrbo") || rMode.contains("trbo") || (rMode.contains("digital") && !rMode.contains("dstar"))
            "fusion" -> rMode.contains("fusion") || rMode.contains("c4fm") || rMode.contains("ysf")
            "d-star" -> rMode.contains("dstar") || rMode.contains("d-star") || rMode.contains("f1d") || rMode.contains("dv")
            "p25" -> rMode.contains("p25") || rMode.contains("apco")
            else -> rMode.contains(fMode)
        }
    }

    val userEquipment: StateFlow<List<ShackEntity>> = shackRepository.equipment()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = RepeaterUiState.Loading
            
            val location = locationService.getCurrentLocation()
            if (location != null) {
                _userLocation.value = Pair(location.latitude, location.longitude)
            }
            
            repository.getAllRepeaters().collectLatest { list ->
                if (list.isEmpty()) {
                    _uiState.value = RepeaterUiState.Empty
                    refreshFromWia()
                } else {
                    updateCalculatedFields(list)
                    _uiState.value = RepeaterUiState.Success
                }
                _isLoading.value = false
            }
        }
    }

    fun refreshFromWia() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.refreshDatabase()
            } catch (e: Exception) {
                _uiState.value = RepeaterUiState.Error(e.message ?: "Failed to refresh from WIA")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateCalculatedFields(list: List<Repeater>) {
        val location = _userLocation.value
        if (location != null) {
            viewModelScope.launch {
                val updated = repository.getNearbyRepeaters(
                    location.first, 
                    location.second, 
                    radius = 5000
                )
                _repeaters.value = updated
            }
        } else {
            _repeaters.value = list
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val location = locationService.getCurrentLocation()
            if (location != null) {
                _userLocation.value = Pair(location.latitude, location.longitude)
                updateCalculatedFields(_repeaters.value)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilters(newFilters: RepeaterFilters) {
        _filters.value = newFilters
    }

    fun toggleFavorite(repeater: Repeater) {
        viewModelScope.launch {
            repository.toggleFavorite(repeater)
        }
    }

    fun markAsRecent(repeater: Repeater) {
        viewModelScope.launch {
            repository.markAsRecent(repeater)
        }
    }

    fun getReachability(repeater: Repeater, equipment: List<ShackEntity>): Reachability {
        if (equipment.isEmpty()) return Reachability.UNKNOWN
        
        val dist = repeater.distance
        val repeaterElevation = repeater.elevation?.toDoubleOrNull() ?: 0.0
        val horizon = 3.57 * (sqrt(2.0) + sqrt(repeaterElevation.coerceAtLeast(0.0)))
        
        return when {
            dist < horizon * 0.8 -> Reachability.EXCELLENT
            dist < horizon * 1.2 -> Reachability.POSSIBLE
            else -> Reachability.UNLIKELY
        }
    }

    fun getRecommendedEquipment(repeater: Repeater, equipment: List<ShackEntity>): ShackEntity? {
        return equipment.find { radio ->
            val matchesBand = repeater.band?.let { b -> radio.notes.contains(b, ignoreCase = true) } ?: true
            val matchesMode = repeater.mode?.let { m -> radio.notes.contains(m, ignoreCase = true) } ?: true
            matchesBand || matchesMode
        } ?: equipment.firstOrNull()
    }
}

data class RepeaterFilters(
    val band: String = "All",
    val mode: String = "All",
    val onlyFavorites: Boolean = false,
    val maxDistance: Int = 5000
)

sealed class RepeaterUiState {
    object Loading : RepeaterUiState()
    object Empty : RepeaterUiState()
    object Success : RepeaterUiState()
    data class Error(val message: String) : RepeaterUiState()
}

enum class Reachability {
    EXCELLENT, POSSIBLE, UNLIKELY, UNKNOWN
}
