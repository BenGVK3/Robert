package au.com.benji.robert.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "club_nets")
data class NetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val frequency: String,
    val dayOfWeek: Int? = null, // 1=Sun, 7=Sat. Null if specific date.
    val specificDate: Long? = null, // Timestamp for one-off events
    val time: String, // HH:mm
    val type: String, // "Net" or "Meeting"
    val notes: String = ""
)
