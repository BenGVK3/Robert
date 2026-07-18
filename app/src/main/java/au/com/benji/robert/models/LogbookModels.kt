package au.com.benji.robert.models

import kotlinx.serialization.Serializable

@Serializable
data class RadioProfile(
    val id: Long = 0,
    val name: String,
    val manufacturer: String = "",
    val maxPower: Double = 100.0,
    val supportedModes: List<String> = emptyList(),
    val supportedBands: List<String> = emptyList(),
    val notes: String = ""
)

@Serializable
data class AntennaProfile(
    val id: Long = 0,
    val name: String,
    val gain: Double = 0.0,
    val type: String = "",
    val polarisation: String = "",
    val notes: String = ""
)

@Serializable
data class OperatorProfile(
    val id: Long = 0,
    val callsign: String,
    val portableCallsign: String = "",
    val name: String = "",
    val defaultRadioId: Long? = null,
    val defaultAntennaId: Long? = null,
    val defaultPower: Double = 100.0,
    val preferredMode: String = "SSB",
    val preferredBands: List<String> = emptyList()
)

@Serializable
data class Activation(
    val type: String, // POTA, SOTA, etc.
    val reference: String,
    val locationName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val imagePath: String = ""
)

@Serializable
data class ActiveActivation(
    val type: String,
    val reference: String,
    val startTime: Long,
    val isPaused: Boolean = false,
    val pauseOffset: Long = 0,
    val currentBand: String = "",
    val currentMode: String = "",
    val locationName: String = ""
)

@Serializable
data class Qso(
    val id: Long = 0,
    val operatorCallsign: String,
    val onAirCallsign: String,
    val callWorked: String,
    val timestamp: Long = System.currentTimeMillis(),
    val frequency: Double, // in MHz
    val band: String,
    val mode: String,
    val rstSent: String = "59",
    val rstReceived: String = "59",
    val name: String = "",
    val qth: String = "",
    val gridsquare: String = "",
    val power: Double = 100.0,
    val radioId: Long? = null,
    val antennaId: Long? = null,
    val notes: String = "",
    
    // QSL Status
    val qslSent: Boolean = false,
    val qslReceived: Boolean = false,
    val lotwStatus: QslStatus = QslStatus.NONE,
    val eqslStatus: QslStatus = QslStatus.NONE,
    val clublogStatus: QslStatus = QslStatus.NONE,
    
    // Activations & References
    val sotaRef: String = "",
    val potaRef: String = "",
    val wwffRef: String = "",
    val hemaRef: String = "",
    val siotaRef: String = "",
    val vkShireRef: String = "",
    val mySotaRef: String = "",
    val myPotaRef: String = "",
    val myWwffRef: String = "",
    val myHemaRef: String = "",
    val mySiotaRef: String = "",
    val myVkShireRef: String = "",
    
    // Advanced / Module fields
    val satelliteName: String = "",
    val satelliteMode: String = "",
    val contestId: String = "",
    val country: String = "",
    val dxcc: String = "",
    val cqZone: Int = 0,
    val ituZone: Int = 0,
    val continent: String = "",
    val stationLat: Double? = null,
    val stationLon: Double? = null
)

enum class QslStatus {
    NONE, SENT, RECEIVED, REQUESTED, IGNORE
}

@Serializable
data class LogbookSettings(
    val defaultOperatorId: Long? = null,
    val autoSave: Boolean = true,
    val autoIncrementTime: Boolean = true,
    val copyPreviousQth: Boolean = false,
    val copyPreviousOperator: Boolean = true,
    val duplicateWarning: Boolean = true,
    val preferredLoggingMode: String = "Normal" // Normal, Contest, Portable
)

@Serializable
data class ServiceCredential(
    val serviceName: String,
    val username: String = "",
    val passwordEncrypted: String = "",
    val apiKeyEncrypted: String = "",
    val isConnected: Boolean = false,
    val lastSync: Long = 0,
    val isEnabled: Boolean = true,
    val priority: Int = 0
)

@Serializable
data class CallsignLookupResult(
    val callsign: String,
    val name: String = "",
    val qth: String = "",
    val gridsquare: String = "",
    val state: String = "",
    val country: String = "",
    val dxcc: String = "",
    val cqZone: Int = 0,
    val ituZone: Int = 0,
    val continent: String = "",
    val flag: String = "",
    val source: String = "", // e.g., "QRZ", "Cache", "History"
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class CallsignHistorySummary(
    val totalQsos: Int = 0,
    val lastWorked: Long = 0,
    val lastBand: String = "",
    val lastMode: String = "",
    val lastRst: String = "",
    val lastNotes: String = ""
)

enum class LookupStatus {
    IDLE, SEARCHING, MATCHED, CACHED, UPDATED, OFFLINE, NO_MATCH
}
