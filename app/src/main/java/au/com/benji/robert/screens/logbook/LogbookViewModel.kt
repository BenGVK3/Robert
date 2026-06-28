package au.com.benji.robert.screens.logbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.database.LogEntryEntity
import au.com.benji.robert.repository.LogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogbookViewModel(
    private val repository: LogRepository
) : ViewModel() {

    val logs: StateFlow<List<LogEntryEntity>> = repository.getAllLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addLog(callsign: String, frequency: String, band: String, mode: String, notes: String = "") {
        viewModelScope.launch {
            repository.addLog(
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
            repository.deleteLog(entry)
        }
    }
}
