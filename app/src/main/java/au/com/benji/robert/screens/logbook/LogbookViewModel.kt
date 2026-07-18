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
import au.com.benji.robert.repository.lookup.CallsignLookupService
import au.com.benji.robert.location.LocationService
import au.com.benji.robert.utils.BandUtils
import au.com.benji.robert.utils.calculateBearing
import au.com.benji.robert.utils.calculateDistance
import au.com.benji.robert.utils.maidenheadToLatLng
import au.com.benji.robert.utils.calculateMaidenhead
import au.com.benji.robert.utils.getCompassDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class LogbookViewModel(application: Application) : AndroidViewModel(application) {

    val repository = DatabaseModule.logbookRepository(application)
    private val dxRepository = DxRepository(DatabaseModule.cacheDao(application))
    private val solarRepository = SolarDataRepository(DatabaseModule.cacheDao(application))
    private val propagationRepository = DatabaseModule.bandConditionsRepository(application)
    private val locationService = LocationService(application)
    
    private val lookupService = CallsignLookupService(
        DatabaseModule.database(application).logbookDao(),
        repository.serviceCredentials
    )

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

    private val _lookupStatus = MutableStateFlow(LookupStatus.IDLE)
    val lookupStatus = _lookupStatus.asStateFlow()

    private val _lookupResult = MutableStateFlow<CallsignLookupResult?>(null)
    val lookupResult = _lookupResult.asStateFlow()

    private val _callHistory = MutableStateFlow<CallsignHistorySummary?>(null)
    val callHistory = _callHistory.asStateFlow()

    private val _callsignInfo = MutableStateFlow<CallsignInfo?>(null)
    val callsignInfo = _callsignInfo.asStateFlow()

    val isOutsideBand = MutableStateFlow(false)
    val isDuplicate = MutableStateFlow(false)
    
    private var lookupJob: Job? = null

    // --- Profiles ---
    val operators = repository.allOperators.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val radios = repository.allRadios.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val antennas = repository.allAntennas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _freqUnit = MutableStateFlow("KHz")
    val freqUnit = _freqUnit.asStateFlow()

    fun updateFreqUnit(unit: String) {
        _freqUnit.value = unit
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(lastFreqUnit = unit))
        }
    }

    init {
        viewModelScope.launch {
            repository.draftQso.first()?.let { _currentQso.value = it }
            
            settings.collect { s ->
                _freqUnit.value = s.lastFreqUnit
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
        var next = update(old)
        
        // Auto-attach active activation
        val active = activeActivation.value
        if (active != null) {
            next = when(active.type) {
                "POTA" -> next.copy(myPotaRef = active.reference)
                "SOTA" -> next.copy(mySotaRef = active.reference)
                "WWFF" -> next.copy(myWwffRef = active.reference)
                else -> next
            }
        }

        // Auto-detect band/mode
        if (next.frequency != old.frequency) {
            val freqKhz = next.frequency * 1000.0
            val band = BandUtils.getBandFromFrequency(freqKhz)
            if (band.isNotEmpty()) {
                val suggestedMode = BandUtils.getSuggestedMode(freqKhz)
                next = next.copy(band = band, mode = if (modeIsAuto(next.mode)) (if (suggestedMode.isNotEmpty()) suggestedMode else next.mode) else next.mode)
                isOutsideBand.value = false
            } else {
                isOutsideBand.value = true
            }
        }

        _currentQso.value = next
        if (settings.value.autoSave) {
            viewModelScope.launch { repository.saveDraft(next) }
        }
    }

    private fun modeIsAuto(mode: String) = mode.isEmpty() || mode == "SSB" || mode == "CW"

    fun onCallsignChanged(call: String) {
        val upper = call.uppercase().trim()
        updateCurrentQso { it.copy(callWorked = upper) }
        
        lookupJob?.cancel()
        if (upper.length < 3) {
            _lookupStatus.value = LookupStatus.IDLE
            _lookupResult.value = null
            _callHistory.value = null
            isDuplicate.value = false
            return
        }

        lookupJob = viewModelScope.launch {
            _lookupStatus.value = LookupStatus.SEARCHING
            
            // 1. Instant Local History
            _callHistory.value = lookupService.getHistorySummary(upper)
            
            // Check for Duplicate
            checkDuplicate(upper)
            
            // 2. Wait for user to finish typing
            delay(500)
            
            // 3. Online Lookup
            val (result, status) = lookupService.lookup(upper)
            _lookupStatus.value = status
            _lookupResult.value = result
            
            result?.let { res ->
                updateCurrentQso { q ->
                    q.copy(
                        name = if (q.name.isEmpty()) res.name else q.name,
                        qth = if (q.qth.isEmpty()) res.qth else q.qth,
                        gridsquare = if (q.gridsquare.isEmpty()) res.gridsquare else q.gridsquare,
                        country = res.country,
                        dxcc = res.dxcc,
                        cqZone = res.cqZone,
                        ituZone = res.ituZone,
                        continent = res.continent
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun checkDuplicate(call: String) {
        val q = _currentQso.value
        val now = System.currentTimeMillis()
        val inLogs = qsos.value.any { 
            it.callWorked.equals(call, ignoreCase = true) && 
            it.band == q.band && 
            it.mode == q.mode && 
            (now - it.timestamp) < 24 * 3600000 
        }
        val inPileUp = _pileUpQueue.value.any { it.equals(call, ignoreCase = true) }
        isDuplicate.value = inLogs || inPileUp
    }

    fun saveQso() {
        viewModelScope.launch {
            val q = _currentQso.value
            val unit = _freqUnit.value
            
            // Convert to MHz for storage
            val freqMhz = when (unit) {
                "KHz" -> q.frequency / 1000.0
                "Hz" -> q.frequency / 1000000.0
                else -> q.frequency
            }
            
            val qToSave = q.copy(frequency = freqMhz)
            repository.insertQso(qToSave)
            
            // Save sticky settings (preserve user's entered value and unit)
            repository.updateSettings(settings.value.copy(
                lastFrequency = q.frequency,
                lastMode = q.mode,
                lastPower = q.power,
                lastFreqUnit = unit
            ))
            
            resetCurrentQso()
        }
    }

    fun clearCurrentQso() {
        viewModelScope.launch { resetCurrentQso() }
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

    fun editQso(qso: Qso) {
        _currentQso.value = qso
    }

    private suspend fun resetCurrentQso() {
        val s = settings.value
        val active = activeActivation.value
        
        _currentQso.value = Qso(
            operatorCallsign = if (s.copyPreviousOperator) _currentQso.value.operatorCallsign else "",
            onAirCallsign = if (s.copyPreviousOperator) _currentQso.value.onAirCallsign else "",
            callWorked = "",
            frequency = s.lastFrequency,
            band = BandUtils.getBandFromFrequency(s.lastFrequency * (if (s.lastFreqUnit == "KHz") 1.0 else if (s.lastFreqUnit == "MHz") 1000.0 else 0.001)),
            mode = s.lastMode,
            power = s.lastPower,
            radioId = _currentQso.value.radioId,
            antennaId = _currentQso.value.antennaId,
            qth = if (s.copyPreviousQth) _currentQso.value.qth else "",
            myPotaRef = if (active?.type == "POTA") active.reference else "",
            mySotaRef = if (active?.type == "SOTA") active.reference else "",
            myWwffRef = if (active?.type == "WWFF") active.reference else ""
        )
        _freqUnit.value = s.lastFreqUnit
        _callsignInfo.value = null
        _lookupStatus.value = LookupStatus.IDLE
        _lookupResult.value = null
        _callHistory.value = null
        isDuplicate.value = false
        repository.saveDraft(null)
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

    // --- Session State ---
    private val _pileUpQueue = MutableStateFlow<List<String>>(emptyList())
    val pileUpQueue = _pileUpQueue.asStateFlow()

    fun addToPileUp(call: String) {
        if (call.isEmpty()) return
        _pileUpQueue.value = _pileUpQueue.value + call.uppercase()
        updateCurrentQso { it.copy(callWorked = "") }
    }

    fun usePileUpCall(call: String) {
        _pileUpQueue.value = _pileUpQueue.value - call
        onCallsignChanged(call)
    }

    fun clearPileUpCall(call: String) {
        _pileUpQueue.value = _pileUpQueue.value - call
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
    
    fun updateCredential(cred: ServiceCredential) {
        viewModelScope.launch {
            val current = repository.serviceCredentials.first()
            val index = current.indexOfFirst { it.serviceName == cred.serviceName }
            val newList = if (index != -1) {
                current.toMutableList().apply { set(index, cred) }
            } else {
                current + cred
            }
            repository.updateCredentials(newList)
        }
    }
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
