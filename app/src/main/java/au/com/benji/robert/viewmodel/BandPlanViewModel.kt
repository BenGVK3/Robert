package au.com.benji.robert.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.models.Band
import au.com.benji.robert.models.BandPlan
import au.com.benji.robert.models.RestrictionType
import au.com.benji.robert.repository.BandPlanRepository
import au.com.benji.robert.repository.SettingsRepository
import kotlinx.coroutines.flow.*

class BandPlanViewModel(
    private val bandPlanRepository: BandPlanRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showOnlyUsableBands = MutableStateFlow(false)
    val showOnlyUsableBands: StateFlow<Boolean> = _showOnlyUsableBands.asStateFlow()

    val userCountry: StateFlow<String> = settingsRepository.country
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Australia")

    val userLicenceClass: StateFlow<String> = settingsRepository.licenceClass
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "foundation")

    val bandPlan: StateFlow<BandPlan?> = userCountry.map { country ->
        bandPlanRepository.getBandPlan(country)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val filteredBands: StateFlow<List<Band>> = combine(
        bandPlan,
        _searchQuery,
        userLicenceClass,
        _showOnlyUsableBands
    ) { plan, query, licence, onlyUsable ->
        var bands = plan?.bands ?: emptyList()

        if (onlyUsable) {
            bands = bands.filter { band ->
                band.allocations.any { allocation ->
                    val restriction = allocation.licenceRestrictions[licence]
                    restriction == RestrictionType.ALLOWED || restriction == RestrictionType.RESTRICTED
                }
            }
        }

        if (query.isBlank()) {
            bands
        } else {
            bands.filter { band ->
                band.name.contains(query, ignoreCase = true) ||
                        band.frequencyRange.start.toString().contains(query) ||
                        band.frequencyRange.end.toString().contains(query)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleOnlyUsableBands(enabled: Boolean) {
        _showOnlyUsableBands.value = enabled
    }

    fun getRestriction(band: Band, licenceId: String): RestrictionType {
        // For simplicity, if any allocation in the band is allowed, we might show something,
        // but the UI should probably show restrictions per allocation.
        // This helper might be for the summary.
        return RestrictionType.ALLOWED 
    }
    
    fun lookupFrequency(frequencyMhz: Double): LookupResult? {
        val plan = bandPlan.value ?: return null
        val band = plan.bands.find { frequencyMhz >= it.frequencyRange.start && frequencyMhz <= it.frequencyRange.end } ?: return null
        val allocation = band.allocations.find { frequencyMhz >= it.frequencyRange.start && frequencyMhz <= it.frequencyRange.end }
        
        val licenceId = userLicenceClass.value
        val restriction = allocation?.licenceRestrictions?.get(licenceId) ?: RestrictionType.NOT_PERMITTED
        val powerLimit = allocation?.powerLimits?.get(licenceId)
        
        return LookupResult(
            bandName = band.name,
            allocationName = allocation?.name ?: "Unknown",
            restriction = restriction,
            modes = allocation?.modes ?: emptyList(),
            powerLimit = powerLimit,
            notes = band.notes
        )
    }
}

data class LookupResult(
    val bandName: String,
    val allocationName: String,
    val restriction: RestrictionType,
    val modes: List<String>,
    val powerLimit: String?,
    val notes: String?
)
