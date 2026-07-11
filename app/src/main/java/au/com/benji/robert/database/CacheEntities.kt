package au.com.benji.robert.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "solar_cache")
data class SolarDataEntity(
    @PrimaryKey val id: Int = 0, // Only one active solar data record
    val solarFlux: Int,
    val kIndex: Int,
    val aIndex: Int,
    val sunspots: Int,
    val muf: String,
    val xRay: String,
    val solarWind: String,
    val protonFlux: String,
    val electronFlux: String,
    val aurora: String,
    val magneticField: String,
    val foF2: String,
    val hfConditionsDay: Map<String, String>,
    val hfConditionsNight: Map<String, String>,
    val vhfAurora: String,
    val eSkip: String,
    val lastUpdated: Long
)

@Entity(tableName = "satellite_cache")
data class SatelliteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lastPositionLat: Double = 0.0,
    val lastPositionLon: Double = 0.0,
    val lastUpdated: Long = 0,
    val isFavorite: Boolean = false
)

@Entity(tableName = "dx_spots_cache")
data class DxSpotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val frequency: String,
    val callsign: String,
    val date: String,
    val de: String,
    val band: String,
    val mode: String,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "aprs_cache")
data class AprsPacketEntity(
    @PrimaryKey val callsign: String,
    val lat: Double,
    val lon: Double,
    val symbol: String,
    val comment: String,
    val timestamp: Long,
    val distance: Double = 0.0,
    val bearing: Double = 0.0
)

@Entity(tableName = "favorite_frequencies")
data class FavoriteFrequencyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val frequency: String,
    val label: String,
    val mode: String = "",
    val band: String = ""
)

@Entity(tableName = "favorite_sdrs")
data class FavoriteSdrEntity(
    @PrimaryKey val url: String,
    val name: String,
    val location: String = ""
)

@Entity(tableName = "callsign_cache")
data class CallsignCacheEntity(
    @PrimaryKey val callsign: String,
    val name: String,
    val qth: String,
    val grid: String,
    val licenceClass: String,
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 0, // Singleton
    val callsign: String = "VK3XYZ",
    val name: String = "",
    val gridSquare: String = "",
    val country: String = "Australia",
    val licenceClass: String = "foundation",
    val kiwisdrUrl: String = "http://kiwisdr.com/public/",
    val homeLat: Double = 0.0,
    val homeLon: Double = 0.0,
    val theme: String = "Auto",
    val lastSync: Long = 0
)
