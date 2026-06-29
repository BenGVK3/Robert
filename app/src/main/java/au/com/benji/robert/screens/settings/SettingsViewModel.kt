package au.com.benji.robert.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val callsign: StateFlow<String> = repository.callsign
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "VK3XYZ")

    val name: StateFlow<String> = repository.name
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Benji")

    val gridSquare: StateFlow<String> = repository.gridSquare
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "QF22og")

    val qrzUsername: StateFlow<String> = repository.qrzUsername
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val qrzPassword: StateFlow<String> = repository.qrzPassword
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun saveSettings(callsign: String, name: String, gridSquare: String) {
        viewModelScope.launch {
            repository.saveSettings(callsign, name, gridSquare)
        }
    }

    fun saveQrzCredentials(username: String, password: String) {
        viewModelScope.launch {
            repository.saveQrzCredentials(username, password)
        }
    }
}
