package au.com.benji.robert.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import au.com.benji.robert.database.*
import au.com.benji.robert.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.logbookDataStore by preferencesDataStore(name = "logbook_settings")

class LogbookRepository(private val dao: LogbookDao, private val context: Context) {

    // Settings Keys
    private val DEFAULT_OPERATOR_ID = longPreferencesKey("default_operator_id")
    private val AUTO_SAVE = booleanPreferencesKey("auto_save")
    private val AUTO_INCREMENT_TIME = booleanPreferencesKey("auto_increment_time")
    private val COPY_PREVIOUS_QTH = booleanPreferencesKey("copy_previous_qth")
    private val COPY_PREVIOUS_OPERATOR = booleanPreferencesKey("copy_previous_operator")
    private val DUPLICATE_WARNING = booleanPreferencesKey("duplicate_warning")
    private val PREFERRED_LOGGING_MODE = stringPreferencesKey("preferred_logging_mode")
    private val DRAFT_QSO = stringPreferencesKey("draft_qso")
    private val ACTIVE_ACTIVATION = stringPreferencesKey("active_activation")
    private val SERVICE_CREDENTIALS = stringPreferencesKey("service_credentials")

    val settings: Flow<LogbookSettings> = context.logbookDataStore.data.map { prefs ->
        LogbookSettings(
            defaultOperatorId = prefs[DEFAULT_OPERATOR_ID],
            autoSave = prefs[AUTO_SAVE] ?: true,
            autoIncrementTime = prefs[AUTO_INCREMENT_TIME] ?: true,
            copyPreviousQth = prefs[COPY_PREVIOUS_QTH] ?: false,
            copyPreviousOperator = prefs[COPY_PREVIOUS_OPERATOR] ?: true,
            duplicateWarning = prefs[DUPLICATE_WARNING] ?: true,
            preferredLoggingMode = prefs[PREFERRED_LOGGING_MODE] ?: "Normal"
        )
    }

    suspend fun updateSettings(newSettings: LogbookSettings) {
        context.logbookDataStore.edit { prefs ->
            newSettings.defaultOperatorId?.let { prefs[DEFAULT_OPERATOR_ID] = it }
            prefs[AUTO_SAVE] = newSettings.autoSave
            prefs[AUTO_INCREMENT_TIME] = newSettings.autoIncrementTime
            prefs[COPY_PREVIOUS_QTH] = newSettings.copyPreviousQth
            prefs[COPY_PREVIOUS_OPERATOR] = newSettings.copyPreviousOperator
            prefs[DUPLICATE_WARNING] = newSettings.duplicateWarning
            prefs[PREFERRED_LOGGING_MODE] = newSettings.preferredLoggingMode
        }
    }

    // Draft Support
    val draftQso: Flow<Qso?> = context.logbookDataStore.data.map { prefs ->
        prefs[DRAFT_QSO]?.let { 
            try {
                kotlinx.serialization.json.Json.decodeFromString<Qso>(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun saveDraft(qso: Qso?) {
        context.logbookDataStore.edit { prefs ->
            if (qso == null) {
                prefs.remove(DRAFT_QSO)
            } else {
                prefs[DRAFT_QSO] = kotlinx.serialization.json.Json.encodeToString(qso)
            }
        }
    }

    // Session Management
    val activeActivation: Flow<ActiveActivation?> = context.logbookDataStore.data.map { prefs ->
        prefs[ACTIVE_ACTIVATION]?.let { 
            try {
                kotlinx.serialization.json.Json.decodeFromString<ActiveActivation>(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun saveActiveActivation(activation: ActiveActivation?) {
        context.logbookDataStore.edit { prefs ->
            if (activation == null) {
                prefs.remove(ACTIVE_ACTIVATION)
            } else {
                prefs[ACTIVE_ACTIVATION] = kotlinx.serialization.json.Json.encodeToString(activation)
            }
        }
    }

    // Credentials Management
    val serviceCredentials: Flow<List<ServiceCredential>> = context.logbookDataStore.data.map { prefs ->
        prefs[SERVICE_CREDENTIALS]?.let { 
            try {
                kotlinx.serialization.json.Json.decodeFromString<List<ServiceCredential>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    suspend fun updateCredentials(creds: List<ServiceCredential>) {
        context.logbookDataStore.edit { prefs ->
            prefs[SERVICE_CREDENTIALS] = kotlinx.serialization.json.Json.encodeToString(creds)
        }
    }

    // QSOs
    val allQsos: Flow<List<Qso>> = dao.getAllQsos().map { entities ->
        entities.map { it.toDomain() }
    }

    fun searchQsos(query: String): Flow<List<Qso>> = dao.searchQsosExtended("%$query%").map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun insertQso(qso: Qso): Long = dao.insertQso(qso.toEntity())
    suspend fun updateQso(qso: Qso) = dao.updateQso(qso.toEntity())
    suspend fun deleteQso(qso: Qso) = dao.deleteQso(qso.toEntity())

    // Radios
    val allRadios: Flow<List<RadioProfile>> = dao.getAllRadios().map { entities ->
        entities.map { it.toDomain() }
    }
    suspend fun insertRadio(radio: RadioProfile) = dao.insertRadio(radio.toEntity())
    suspend fun updateRadio(radio: RadioProfile) = dao.updateRadio(radio.toEntity())
    suspend fun deleteRadio(radio: RadioProfile) = dao.deleteRadio(radio.toEntity())

    // Antennas
    val allAntennas: Flow<List<AntennaProfile>> = dao.getAllAntennas().map { entities ->
        entities.map { it.toDomain() }
    }
    suspend fun insertAntenna(antenna: AntennaProfile) = dao.insertAntenna(antenna.toEntity())
    suspend fun updateAntenna(antenna: AntennaProfile) = dao.updateAntenna(antenna.toEntity())
    suspend fun deleteAntenna(antenna: AntennaProfile) = dao.deleteAntenna(antenna.toEntity())

    // Operators
    val allOperators: Flow<List<OperatorProfile>> = dao.getAllOperators().map { entities ->
        entities.map { it.toDomain() }
    }
    suspend fun getOperatorById(id: Long) = dao.getOperatorById(id)?.toDomain()
    suspend fun insertOperator(operator: OperatorProfile) = dao.insertOperator(operator.toEntity())
    suspend fun updateOperator(operator: OperatorProfile) = dao.updateOperator(operator.toEntity())
    suspend fun deleteOperator(operator: OperatorProfile) = dao.deleteOperator(operator.toEntity())

    // Export Logic
    fun exportToAdif(qsos: List<Qso>): String {
        val sb = StringBuilder()
        sb.append("ADIF Export from Robert\n")
        sb.append("<ADIF_VER:5>3.1.4\n")
        sb.append("<PROGRAMID:6>Robert\n")
        sb.append("<EOH>\n\n")

        val dateFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        }
        val timeFormat = java.text.SimpleDateFormat("HHmmss", java.util.Locale.US).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        }

        for (qso in qsos) {
            val date = dateFormat.format(java.util.Date(qso.timestamp))
            val time = timeFormat.format(java.util.Date(qso.timestamp))

            sb.append("<CALL:${qso.callWorked.length}>${qso.callWorked}")
            sb.append("<QSO_DATE:${date.length}>$date")
            sb.append("<TIME_ON:${time.length}>$time")
            sb.append("<FREQ:${qso.frequency.toString().length}>${qso.frequency}")
            sb.append("<BAND:${qso.band.length}>${qso.band}")
            sb.append("<MODE:${qso.mode.length}>${qso.mode}")
            sb.append("<RST_SENT:${qso.rstSent.length}>${qso.rstSent}")
            sb.append("<RST_RCVD:${qso.rstReceived.length}>${qso.rstReceived}")
            
            if (qso.name.isNotEmpty()) sb.append("<NAME:${qso.name.length}>${qso.name}")
            if (qso.qth.isNotEmpty()) sb.append("<QTH:${qso.qth.length}>${qso.qth}")
            if (qso.gridsquare.isNotEmpty()) sb.append("<GRIDSQUARE:${qso.gridsquare.length}>${qso.gridsquare}")
            if (qso.power > 0) sb.append("<TX_PWR:${qso.power.toString().length}>${qso.power}")
            if (qso.operatorCallsign.isNotEmpty()) sb.append("<OPERATOR:${qso.operatorCallsign.length}>${qso.operatorCallsign}")
            if (qso.onAirCallsign.isNotEmpty()) sb.append("<STATION_CALLSIGN:${qso.onAirCallsign.length}>${qso.onAirCallsign}")
            
            if (qso.sotaRef.isNotEmpty()) sb.append("<SOTA_REF:${qso.sotaRef.length}>${qso.sotaRef}")
            if (qso.potaRef.isNotEmpty()) sb.append("<POTA_REF:${qso.potaRef.length}>${qso.potaRef}")
            if (qso.wwffRef.isNotEmpty()) sb.append("<WWFF_REF:${qso.wwffRef.length}>${qso.wwffRef}")
            if (qso.vkShireRef.isNotEmpty()) sb.append("<MY_VUCC_GRIDS:${qso.vkShireRef.length}>${qso.vkShireRef}") // Mapping VK Shire to ADIF?
            
            if (qso.notes.isNotEmpty()) sb.append("<NOTES:${qso.notes.length}>${qso.notes}")
            
            sb.append("<EOR>\n")
        }
        return sb.toString()
    }

    fun exportToCabrillo(qsos: List<Qso>, operator: String = ""): String {
        val sb = StringBuilder()
        sb.append("START-OF-LOG: 3.0\n")
        sb.append("LOCATION: DX\n")
        sb.append("CALLSIGN: $operator\n")
        sb.append("CATEGORY-OPERATOR: SINGLE-OP\n")
        sb.append("CATEGORY-ASSISTED: NON-ASSISTED\n")
        sb.append("CATEGORY-BAND: ALL\n")
        sb.append("CATEGORY-MODE: MIXED\n")
        sb.append("CATEGORY-POWER: HIGH\n")
        sb.append("CATEGORY-STATION: FIXED\n")
        sb.append("CATEGORY-TRANSMITTER: ONE\n")
        sb.append("CLUB: Robert Radio Users\n")
        sb.append("CREATED-BY: Robert\n")
        sb.append("NAME: Robert User\n")

        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        }
        val timeFormat = java.text.SimpleDateFormat("HHmm", java.util.Locale.US).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        }

        for (qso in qsos) {
            val date = dateFormat.format(java.util.Date(qso.timestamp))
            val time = timeFormat.format(java.util.Date(qso.timestamp))
            val freq = (qso.frequency * 1000).toInt() // kHz for Cabrillo often
            
            // QSO: freq mo date time mycall rst rcvd call rst rcvd
            sb.append("QSO: %5d %2s %s %s %-12s %3s %-6s %-12s %3s %-6s\n".format(
                java.util.Locale.US,
                freq, qso.mode.take(2), date, time, qso.onAirCallsign, qso.rstSent, "", qso.callWorked, qso.rstReceived, ""
            ))
        }
        sb.append("END-OF-LOG:\n")
        return sb.toString()
    }

    fun exportToCsv(qsos: List<Qso>): String {
        val sb = StringBuilder()
        sb.append("CallWorked,Timestamp,Frequency,Band,Mode,RstSent,RstReceived,Name,QTH,GridSquare,Power,Operator,PotaRef,SotaRef,Notes\n")
        
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        }

        for (qso in qsos) {
            sb.append("${qso.callWorked},")
            sb.append("${dateFormat.format(java.util.Date(qso.timestamp))},")
            sb.append("${qso.frequency},")
            sb.append("${qso.band},")
            sb.append("${qso.mode},")
            sb.append("${qso.rstSent},")
            sb.append("${qso.rstReceived},")
            sb.append("\"${qso.name.replace("\"", "\"\"")}\",")
            sb.append("\"${qso.qth.replace("\"", "\"\"")}\",")
            sb.append("${qso.gridsquare},")
            sb.append("${qso.power},")
            sb.append("${qso.operatorCallsign},")
            sb.append("${qso.potaRef},")
            sb.append("${qso.sotaRef},")
            sb.append("\"${qso.notes.replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }

    fun importFromAdif(adif: String): List<Qso> {
        val qsos = mutableListOf<Qso>()
        val records = adif.split(Regex("<EOR>", RegexOption.IGNORE_CASE))
        
        val dateFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        }
        val timeFormat = java.text.SimpleDateFormat("HHmmss", java.util.Locale.US).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        }

        for (record in records) {
            if (record.trim().isEmpty()) continue
            
            val call = extractAdifField(record, "CALL") ?: continue
            val dateStr = extractAdifField(record, "QSO_DATE") ?: ""
            val timeStr = (extractAdifField(record, "TIME_ON") ?: "000000").padEnd(6, '0')
            val band = extractAdifField(record, "BAND") ?: ""
            val mode = extractAdifField(record, "MODE") ?: ""
            val freq = extractAdifField(record, "FREQ")?.toDoubleOrNull() ?: 0.0
            
            val timestamp = try {
                val combined = "${dateStr}${timeStr}"
                if (combined.length >= 14) {
                    java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US).apply { 
                        timeZone = java.util.TimeZone.getTimeZone("UTC") 
                    }.parse(combined)?.time ?: System.currentTimeMillis()
                } else {
                    System.currentTimeMillis()
                }
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            qsos.add(Qso(
                callWorked = call,
                timestamp = timestamp,
                band = band,
                mode = mode,
                frequency = freq,
                operatorCallsign = extractAdifField(record, "OPERATOR") ?: "",
                onAirCallsign = extractAdifField(record, "STATION_CALLSIGN") ?: "",
                rstSent = extractAdifField(record, "RST_SENT") ?: "59",
                rstReceived = extractAdifField(record, "RST_RCVD") ?: "59",
                name = extractAdifField(record, "NAME") ?: "",
                qth = extractAdifField(record, "QTH") ?: "",
                gridsquare = extractAdifField(record, "GRIDSQUARE") ?: "",
                power = extractAdifField(record, "TX_PWR")?.toDoubleOrNull() ?: 0.0,
                notes = extractAdifField(record, "NOTES") ?: "",
                sotaRef = extractAdifField(record, "SOTA_REF") ?: "",
                potaRef = extractAdifField(record, "POTA_REF") ?: "",
                wwffRef = extractAdifField(record, "WWFF_REF") ?: ""
            ))
        }
        return qsos
    }

    private fun extractAdifField(record: String, field: String): String? {
        val pattern = Regex("<$field:(\\d+)>([^<]*)", RegexOption.IGNORE_CASE)
        val match = pattern.find(record)
        return match?.groupValues?.get(2)?.trim()
    }

    // Mappers
    private fun QsoEntity.toDomain() = Qso(
        id = id, operatorCallsign = operatorCallsign, onAirCallsign = onAirCallsign, callWorked = callWorked,
        timestamp = timestamp, frequency = frequency, band = band, mode = mode, rstSent = rstSent, rstReceived = rstReceived,
        name = name, qth = qth, gridsquare = gridsquare, power = power, radioId = radioId, antennaId = antennaId,
        notes = notes, qslSent = qslSent, qslReceived = qslReceived, lotwStatus = lotwStatus, eqslStatus = eqslStatus,
        clublogStatus = clublogStatus, sotaRef = sotaRef, potaRef = potaRef, wwffRef = wwffRef, hemaRef = hemaRef,
        siotaRef = siotaRef, vkShireRef = vkShireRef, mySotaRef = mySotaRef, myPotaRef = myPotaRef, myWwffRef = myWwffRef,
        myHemaRef = myHemaRef, mySiotaRef = mySiotaRef, myVkShireRef = myVkShireRef, satelliteName = satelliteName,
        satelliteMode = satelliteMode, contestId = contestId, country = country, dxcc = dxcc, cqZone = cqZone,
        ituZone = ituZone, continent = continent, stationLat = stationLat, stationLon = stationLon
    )

    private fun Qso.toEntity() = QsoEntity(
        id = id, operatorCallsign = operatorCallsign, onAirCallsign = onAirCallsign, callWorked = callWorked,
        timestamp = timestamp, frequency = frequency, band = band, mode = mode, rstSent = rstSent, rstReceived = rstReceived,
        name = name, qth = qth, gridsquare = gridsquare, power = power, radioId = radioId, antennaId = antennaId,
        notes = notes, qslSent = qslSent, qslReceived = qslReceived, lotwStatus = lotwStatus, eqslStatus = eqslStatus,
        clublogStatus = clublogStatus, sotaRef = sotaRef, potaRef = potaRef, wwffRef = wwffRef, hemaRef = hemaRef,
        siotaRef = siotaRef, vkShireRef = vkShireRef, mySotaRef = mySotaRef, myPotaRef = myPotaRef, myWwffRef = myWwffRef,
        myHemaRef = myHemaRef, mySiotaRef = mySiotaRef, myVkShireRef = myVkShireRef, satelliteName = satelliteName,
        satelliteMode = satelliteMode, contestId = contestId, country = country, dxcc = dxcc, cqZone = cqZone,
        ituZone = ituZone, continent = continent, stationLat = stationLat, stationLon = stationLon
    )

    private fun RadioProfileEntity.toDomain() = RadioProfile(
        id = id, name = name, manufacturer = manufacturer, maxPower = maxPower, supportedModes = supportedModes,
        supportedBands = supportedBands, notes = notes
    )

    private fun RadioProfile.toEntity() = RadioProfileEntity(
        id = id, name = name, manufacturer = manufacturer, maxPower = maxPower, supportedModes = supportedModes,
        supportedBands = supportedBands, notes = notes
    )

    private fun AntennaProfileEntity.toDomain() = AntennaProfile(
        id = id, name = name, gain = gain, type = type, polarisation = polarisation, notes = notes
    )

    private fun AntennaProfile.toEntity() = AntennaProfileEntity(
        id = id, name = name, gain = gain, type = type, polarisation = polarisation, notes = notes
    )

    private fun OperatorProfileEntity.toDomain() = OperatorProfile(
        id = id, callsign = callsign, portableCallsign = portableCallsign, name = name, defaultRadioId = defaultRadioId,
        defaultAntennaId = defaultAntennaId, defaultPower = defaultPower, preferredMode = preferredMode,
        preferredBands = preferredBands
    )

    private fun OperatorProfile.toEntity() = OperatorProfileEntity(
        id = id, callsign = callsign, portableCallsign = portableCallsign, name = name, defaultRadioId = defaultRadioId,
        defaultAntennaId = defaultAntennaId, defaultPower = defaultPower, preferredMode = preferredMode,
        preferredBands = preferredBands
    )
}
