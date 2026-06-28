package au.com.benji.robert.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logbook")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callsign: String,
    val frequency: String,
    val band: String,
    val mode: String,
    val rstSent: String = "59",
    val rstReceived: String = "59",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
