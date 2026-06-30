package au.com.benji.robert.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logbook")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callsign: String,
    val name: String = "",
    val qth: String = "",
    val frequency: String, // Stored in kHz as requested
    val band: String,
    val mode: String,
    val rstSent: String = "59",
    val rstReceived: String = "59",
    val power: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    
    // References
    val sotaRef: String = "",
    val potaRef: String = "",
    val wwffRef: String = "",
    val hemaRef: String = "",
    val siotaRef: String = "",
    val vkShireRef: String = ""
)
