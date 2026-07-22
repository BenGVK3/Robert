package au.com.benji.robert.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "propagation_history")
data class PropagationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val band: String,
    val score: Int,
    
    // Detailed context for advanced graphing/prediction
    val muf: Double = 0.0,
    val sfi: Int = 0,
    val kIndex: Int = 0,
    val aIndex: Int = 0,
    val solarElevation: Double = 0.0,
    val confidence: Int = 0
)
