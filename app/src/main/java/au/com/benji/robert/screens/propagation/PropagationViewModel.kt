package au.com.benji.robert.screens.propagation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.database.PropagationSpotEntity
import au.com.benji.robert.repository.propagation.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PropagationViewModel(application: Application) : AndroidViewModel(application) {
    private val propagationDao = DatabaseModule.propagationDao(application)
    private val providers = listOf(PskReporterProvider())

    private val _state = MutableStateFlow(PropagationState())
    val state: StateFlow<PropagationState> = _state.asStateFlow()

    private val _selectedBand = MutableStateFlow("20m")
    val selectedBand = _selectedBand.asStateFlow()

    private val _selectedMode = MutableStateFlow("FT8")
    val selectedMode = _selectedMode.asStateFlow()

    private val _selectedTimeWindow = MutableStateFlow(15) // minutes
    val selectedTimeWindow = _selectedTimeWindow.asStateFlow()

    private var refreshJob: Job? = null

    init {
        startAutoRefresh()
        observeSpots()
    }

    private fun observeSpots() {
        combine(_selectedBand, _selectedTimeWindow) { band, window ->
            Pair(band, window)
        }.flatMapLatest { (band, window) ->
            val minTimestamp = System.currentTimeMillis() - (window * 60 * 1000)
            propagationDao.getSpotsByBand(band, minTimestamp)
        }.onEach { entities ->
            _state.update { it.copy(spots = entities.map { e -> e.toDomainModel() }) }
        }.catch { e ->
            _state.update { it.copy(error = e.message) }
        }.launchIn(viewModelScope)
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                fetchLatestSpots()
                delay(5 * 60 * 1000) // Refresh every 5 minutes
            }
        }
    }

    fun manualRefresh() {
        viewModelScope.launch {
            fetchLatestSpots()
        }
    }

    private suspend fun fetchLatestSpots() {
        _state.update { it.copy(isLoading = true, error = null) }
        val band = _selectedBand.value
        val mode = _selectedMode.value
        val window = _selectedTimeWindow.value

        try {
            val allSpots = mutableListOf<PropagationSpot>()
            providers.forEach { provider ->
                try {
                    val spots = provider.fetchSpots(band, mode, window)
                    allSpots.addAll(spots)
                } catch (e: Exception) {
                    // Handle provider failure gracefully
                }
            }

            if (allSpots.isNotEmpty()) {
                propagationDao.insertSpots(allSpots.map { PropagationSpotEntity.fromDomainModel(it) })
            }
            
            // Clean up old spots (older than 24h)
            val threshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            propagationDao.clearOldSpots(threshold)

            _state.update { 
                it.copy(
                    isLoading = false, 
                    lastUpdate = System.currentTimeMillis(),
                    activeProviders = providers.map { p -> p.name }
                ) 
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    fun setBand(band: String) {
        _selectedBand.value = band
        manualRefresh()
    }

    fun setMode(mode: String) {
        _selectedMode.value = mode
        manualRefresh()
    }

    fun setTimeWindow(minutes: Int) {
        _selectedTimeWindow.value = minutes
        manualRefresh()
    }
}
