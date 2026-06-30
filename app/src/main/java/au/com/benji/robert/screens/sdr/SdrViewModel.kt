package au.com.benji.robert.screens.sdr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SdrViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    val kiwisdrUrl: StateFlow<String> = settingsRepository.kiwisdrUrl
        .map { url ->
            var cleaned = url.trim()
                .replace("hppt://", "http://", ignoreCase = true)
                .replace("kiwiSDR", "kiwisdr.com", ignoreCase = true)
            
            // If it's a parking page or broken, reset to default
            if (cleaned.isBlank() || cleaned.contains(".xyz") || cleaned == "about:blank") {
                cleaned = "http://kiwisdr.com/public/"
            }
            cleaned
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "http://kiwisdr.com/public/"
        )

    fun saveUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank() || trimmed == "about:blank" || trimmed.contains(".xyz")) return
        
        // Only save valid KiwiSDR related URLs
        if (trimmed.contains("kiwisdr.com", ignoreCase = true) || 
            trimmed.contains(":8073") || 
            trimmed.contains("kiwi-sdr", ignoreCase = true)) {
            
            viewModelScope.launch {
                settingsRepository.saveKiwisdrUrl(trimmed)
            }
        }
    }
}
