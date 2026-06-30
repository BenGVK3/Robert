package au.com.benji.robert.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repeaters", primaryKeys = ["callsign", "frequency"])
data class RepeaterEntity(
    val callsign: String,
    val frequency: String, // Output Frequency
    val name: String?,
    val inputFreq: String?,
    val offset: String,
    val band: String?,
    val mode: String?,
    val tone: String?, // CTCSS
    val dcs: String?,
    val location: String?,
    val town: String?,
    val state: String?,
    val country: String? = "Australia",
    val lat: Double,
    val lng: Double,
    val gridSquare: String?,
    val elevation: String?,
    val notes: String?,
    val status: String?,
    val lastUpdate: String?,
    
    // Application state
    val isFavorite: Boolean = false,
    val isRecent: Boolean = false,
    val lastViewed: Long = 0,
    val maidenhead: String = "", // Calculated or from CSV
    
    // Networking (optional/future proofing)
    val echolinkId: String? = null,
    val irlpId: String? = null,
    val allstarNode: String? = null,
    val wiresX: String? = null,
    val dmrId: String? = null,
    val colorCode: String? = null,
    val timeSlot: String? = null,
    val talkgroup: String? = null
)
