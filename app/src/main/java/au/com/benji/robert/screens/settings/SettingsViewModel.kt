package au.com.benji.robert.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.repository.BandPlanRepository
import au.com.benji.robert.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val bandPlanRepository: BandPlanRepository
) : ViewModel() {

    val callsign: StateFlow<String> = repository.callsign
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "VK3XYZ")

    val name: StateFlow<String> = repository.name
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Benji")

    val gridSquare: StateFlow<String> = repository.gridSquare
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "QF22og")

    val country: StateFlow<String> = repository.country
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Australia")

    val licenceClass: StateFlow<String> = repository.licenceClass
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "foundation")

    fun getCountries() = bandPlanRepository.getSupportedCountries()

    fun getLicenceClasses(country: String): List<au.com.benji.robert.models.LicenceClass> {
        return bandPlanRepository.getBandPlan(country)?.licenceClasses ?: emptyList()
    }

    fun saveSettings(callsign: String, name: String, gridSquare: String, country: String, licenceClass: String) {
        viewModelScope.launch {
            repository.saveSettings(callsign, name, gridSquare, country, licenceClass)
        }
    }
}
