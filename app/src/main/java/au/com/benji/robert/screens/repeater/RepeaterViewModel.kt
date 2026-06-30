package au.com.benji.robert.screens.repeater

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.database.DatabaseProvider
import au.com.benji.robert.database.ShackEntity
import au.com.benji.robert.location.LocationService
import au.com.benji.robert.models.Repeater
import au.com.benji.robert.repository.RepeaterRepository
import au.com.benji.robert.repository.ShackRepository
import au.com.benji.robert.utils.calculateMaidenhead
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class RepeaterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RepeaterRepository(application)
    private val shackRepository = ShackRepository(DatabaseProvider.getDatabase(application).shackDao())
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
                repeater.band?.contains(filter.band, ignoreCase = true) == true ||
                repeater.frequency.toBand() == filter.band
                
            val matchesMode = filter.mode == "All" || 
                repeater.mode?.contains(filter.mode, ignoreCase = true) == true
                
            val matchesFavorite = !filter.onlyFavorites || repeater.isFavorite
            val matchesDistance = repeater.distance <= filter.maxDistance
            
            matchesSearch && matchesBand && matchesMode && matchesFavorite && matchesDistance
        }.sortedBy { it.distance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    
    private fun String.toBand(): String {
        val freq = this.toDoubleOrNull() ?: return ""
        return when {
            freq in 28.0..29.7 -> "10m"
            freq in 50.0..54.0 -> "6m"
            freq in 144.0..148.0 -> "2m"
            freq in 420.0..450.0 -> "70cm"
            freq in 1240.0..1300.0 -> "23cm"
            else -> ""
        }
    }
}

data class RepeaterFilters(
    val band: String = "All",
    val mode: String = "All",
    val onlyFavorites: Boolean = false,
    val maxDistance: Int = 200
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
