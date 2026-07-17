package au.com.benji.robert.screens.logbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.models.*
import au.com.benji.robert.repository.LogbookRepository
import au.com.benji.robert.repository.DxRepository
import au.com.benji.robert.repository.SolarDataRepository
import au.com.benji.robert.repository.propagation.PropagationRepository
import au.com.benji.robert.location.LocationService
import au.com.benji.robert.utils.BandUtils
import au.com.benji.robert.utils.calculateBearing
import au.com.benji.robert.utils.calculateDistance
import au.com.benji.robert.utils.maidenheadToLatLng
import au.com.benji.robert.utils.calculateMaidenhead
import au.com.benji.robert.utils.getCompassDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class LogbookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DatabaseModule.logbookRepository(application)
    private val dxRepository = DxRepository(DatabaseModule.cacheDao(application))
    private val solarRepository = SolarDataRepository(DatabaseModule.cacheDao(application))
    private val propagationRepository = DatabaseModule.bandConditionsRepository(application)
    private val locationService = LocationService(application)

    // --- Core State ---
    val settings = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogbookSettings())
    val qsos = repository.allQsos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeActivation = repository.activeActivation.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val filteredQsos = _searchQuery.flatMapLatest { query ->
        if (query.isEmpty()) repository.allQsos
        else repository.searchQsos(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Activation & Session State ---
    val isActivationActive = activeActivation.map { it != null }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val sessionQsos = combine(qsos, activeActivation) { all, active ->
        if (active == null) emptyList()
        else all.filter { it.myPotaRef == active.reference || it.mySotaRef == active.reference || it.myWwffRef == active.reference }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val elapsedTime = activeActivation.flatMapLatest { active ->
        if (active == null) flowOf(0L)
        else flow {
            while (true) {
                if (!active.isPaused) {
                    emit(System.currentTimeMillis() - active.startTime - active.pauseOffset)
                }
                delay(1000)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // --- Logging State ---
    private val _currentQso = MutableStateFlow(Qso(frequency = 14.200, band = "20m", mode = "SSB", operatorCallsign = "", onAirCallsign = "", callWorked = ""))
    val currentQso = _currentQso.asStateFlow()

    private val _callsignInfo = MutableStateFlow<CallsignInfo?>(null)
    val callsignInfo = _callsignInfo.asStateFlow()

    val isOutsideBand = MutableStateFlow(false)
    val isDuplicate = MutableStateFlow(false)

    // --- Profiles ---
    val operators = repository.allOperators.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val radios = repository.allRadios.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val antennas = repository.allAntennas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.draftQso.first()?.let { _currentQso.value = it }
            
            settings.collect { s ->
                s.defaultOperatorId?.let { id ->
                    repository.getOperatorById(id)?.let { op ->
                        updateCurrentQso { it.copy(operatorCallsign = op.callsign, onAirCallsign = op.callsign) }
                    }
                }
            }
        }
    }

    // --- Actions ---

    fun updateCurrentQso(update: (Qso) -> Qso) {
        val old = _currentQso.value
        val next = update(old)
        
        // Auto-attach active activation
        val active = activeActivation.value
        var finalQso = if (active != null) {
            when(active.type) {
                "POTA" -> next.copy(myPotaRef = active.reference)
                "SOTA" -> next.copy(mySotaRef = active.reference)
                "WWFF" -> next.copy(myWwffRef = active.reference)
                else -> next
            }
        } else next

        // Auto-detect band/mode
        if (finalQso.frequency != old.frequency) {
            val freqKhz = finalQso.frequency * 1000.0
            val band = BandUtils.getBandFromFrequency(freqKhz)
            if (band.isNotEmpty()) {
                val mode = BandUtils.getSuggestedMode(freqKhz)
                finalQso = finalQso.copy(band = band, mode = if (mode.isNotEmpty()) mode else finalQso.mode)
                isOutsideBand.value = false
            } else {
                isOutsideBand.value = true
            }
        }

        _currentQso.value = finalQso
        if (settings.value.autoSave) {
            viewModelScope.launch { repository.saveDraft(finalQso) }
        }
    }

    fun onCallsignChanged(call: String) {
        val upper = call.uppercase().trim()
        updateCurrentQso { it.copy(callWorked = upper) }
        
        if (upper.length >= 3) {
            viewModelScope.launch {
                val dxcc = au.com.benji.robert.utils.HamUtils.getDxccInfo(upper)
                _callsignInfo.value = CallsignInfo(
                    callsign = upper,
                    country = dxcc?.country ?: "Unknown",
                    prefix = dxcc?.prefix ?: "",
                    cqZone = dxcc?.cqZone ?: 0,
                    flag = dxcc?.flagEmoji ?: ""
                )
                checkDuplicate(upper)
            }
        } else {
            _callsignInfo.value = null
            isDuplicate.value = false
        }
    }

    private fun checkDuplicate(call: String) {
        val q = _currentQso.value
        isDuplicate.value = qsos.value.any { 
            it.callWorked.equals(call, ignoreCase = true) && it.band == q.band && it.mode == q.mode && (System.currentTimeMillis() - it.timestamp) < 24 * 3600000
        }
    }

    fun saveQso() {
        viewModelScope.launch {
            repository.insertQso(_currentQso.value)
            resetCurrentQso()
        }
    }

    fun deleteQso(qso: Qso) {
        viewModelScope.launch {
            repository.deleteQso(qso)
        }
    }

    fun duplicateQso(qso: Qso) {
        viewModelScope.launch {
            val newQso = qso.copy(id = 0, timestamp = System.currentTimeMillis())
            repository.insertQso(newQso)
        }
    }

    fun resetCurrentQso() {
        viewModelScope.launch {
            val s = settings.value
            val active = activeActivation.value
            val prev = _currentQso.value
            
            _currentQso.value = Qso(
                operatorCallsign = if (s.copyPreviousOperator) prev.operatorCallsign else "",
                onAirCallsign = if (s.copyPreviousOperator) prev.onAirCallsign else "",
                callWorked = "",
                frequency = prev.frequency,
                band = prev.band,
                mode = prev.mode,
                power = prev.power,
                radioId = prev.radioId,
                antennaId = prev.antennaId,
                qth = if (s.copyPreviousQth) prev.qth else "",
                myPotaRef = if (active?.type == "POTA") active.reference else "",
                mySotaRef = if (active?.type == "SOTA") active.reference else "",
                myWwffRef = if (active?.type == "WWFF") active.reference else ""
            )
            _callsignInfo.value = null
            isDuplicate.value = false
            repository.saveDraft(null)
        }
    }

    // --- Session Control ---
    fun startActivation(type: String, ref: String, location: String) {
        viewModelScope.launch {
            val session = ActiveActivation(type, ref, System.currentTimeMillis(), locationName = location)
            repository.saveActiveActivation(session)
        }
    }

    fun pauseActivation() {
        viewModelScope.launch {
            activeActivation.value?.let { 
                repository.saveActiveActivation(it.copy(isPaused = !it.isPaused))
            }
        }
    }

    fun finishActivation() {
        viewModelScope.launch {
            repository.saveActiveActivation(null)
        }
    }

    // --- Statistics ---
    val stats = qsos.map { all ->
        val now = Calendar.getInstance()
        val today = all.count { isSameDay(it.timestamp, now.timeInMillis) }
        val dxcc = all.map { it.dxcc }.distinct().filter { it.isNotEmpty() }.size
        val topBand = all.groupBy { it.band }.maxByOrNull { it.value.size }?.key ?: "---"
        
        LogbookStats(today, dxcc, topBand, all.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogbookStats())

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    // --- Profile Management ---
    fun addOperator(op: OperatorProfile) = viewModelScope.launch { repository.insertOperator(op) }
    fun deleteOperator(op: OperatorProfile) = viewModelScope.launch { repository.deleteOperator(op) }
    fun addRadio(r: RadioProfile) = viewModelScope.launch { repository.insertRadio(r) }
    fun deleteRadio(r: RadioProfile) = viewModelScope.launch { repository.deleteRadio(r) }
    fun addAntenna(a: AntennaProfile) = viewModelScope.launch { repository.insertAntenna(a) }
    fun deleteAntenna(a: AntennaProfile) = viewModelScope.launch { repository.deleteAntenna(a) }

    fun updateSettings(s: LogbookSettings) = viewModelScope.launch { repository.updateSettings(s) }
    
    fun fetchMyGrid() {
        viewModelScope.launch {
            locationService.getCurrentLocation()?.let {
                updateCurrentQso { q -> q.copy(gridsquare = calculateMaidenhead(it.latitude, it.longitude)) }
            }
        }
    }

    val awardsProgress = qsos.map { all ->
        mapOf(
            "DXCC" to all.map { it.dxcc }.distinct().filter { it.isNotEmpty() }.size,
            "Continents" to all.map { it.continent }.distinct().filter { it.isNotEmpty() }.size,
            "Bands" to all.map { it.band }.distinct().filter { it.isNotEmpty() }.size,
            "Modes" to all.map { it.mode }.distinct().filter { it.isNotEmpty() }.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap<String, Int>())

    val dailyActivity = qsos.map { all ->
        val dateFormat = java.text.SimpleDateFormat("MMM d", java.util.Locale.US)
        List(7) { i ->
            val target = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -i) }
            val label = dateFormat.format(target.time)
            val count = all.count { qso ->
                val qsoCal = java.util.Calendar.getInstance().apply { timeInMillis = qso.timestamp }
                qsoCal.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR) &&
                qsoCal.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR)
            }
            label to count
        }.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun exportLogs(): String = repository.exportToAdif(qsos.value)
    fun exportCabrillo(): String = repository.exportToCabrillo(qsos.value, _currentQso.value.operatorCallsign)
    fun exportCsv(): String = repository.exportToCsv(qsos.value)
}

data class LogbookStats(
    val today: Int = 0,
    val dxcc: Int = 0,
    val topBand: String = "---",
    val total: Int = 0
)

data class CallsignInfo(
    val callsign: String,
    val country: String,
    val prefix: String,
    val cqZone: Int,
    val flag: String
)
