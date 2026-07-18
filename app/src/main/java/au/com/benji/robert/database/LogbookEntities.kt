package au.com.benji.robert.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import au.com.benji.robert.models.QslStatus

@Entity(tableName = "radio_profiles")
data class RadioProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val manufacturer: String = "",
    val maxPower: Double = 100.0,
    val supportedModes: List<String> = emptyList(),
    val supportedBands: List<String> = emptyList(),
    val notes: String = ""
)

@Entity(tableName = "antenna_profiles")
data class AntennaProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val gain: Double = 0.0,
    val type: String = "",
    val polarisation: String = "",
    val notes: String = ""
)

@Entity(tableName = "operator_profiles")
data class OperatorProfileEntity(
    @PrimaryKey(autoGenerate = true)
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

@Entity(
    tableName = "qsos",
    indices = [
        Index(value = ["callWorked"]),
        Index(value = ["timestamp"]),
        Index(value = ["band"]),
        Index(value = ["mode"])
    ]
)
data class QsoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operatorCallsign: String,
    val onAirCallsign: String,
    val callWorked: String,
    val timestamp: Long = System.currentTimeMillis(),
    val frequency: Double,
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
    
    val qslSent: Boolean = false,
    val qslReceived: Boolean = false,
    val lotwStatus: QslStatus = QslStatus.NONE,
    val eqslStatus: QslStatus = QslStatus.NONE,
    val clublogStatus: QslStatus = QslStatus.NONE,
    
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

data class CallsignHistorySummaryEntity(
    val total: Int,
    val lastWorked: Long,
    val lastBand: String,
    val lastMode: String,
    val lastRst: String
)
