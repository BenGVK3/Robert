package au.com.benji.robert.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheDao {

    // Solar
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSolar(data: SolarDataEntity)

    @Query("SELECT * FROM solar_cache WHERE id = 0")
    fun getSolarFlow(): Flow<SolarDataEntity?>

    @Query("SELECT * FROM solar_cache WHERE id = 0")
    suspend fun getSolarSync(): SolarDataEntity?

    // Satellites
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSatellites(satellites: List<SatelliteEntity>)

    @Query("SELECT * FROM satellite_cache")
    fun getAllSatellites(): Flow<List<SatelliteEntity>>

    @Query("SELECT * FROM satellite_cache WHERE isFavorite = 1")
    fun getFavoriteSatellites(): Flow<List<SatelliteEntity>>

    @Update
    suspend fun updateSatellite(satellite: SatelliteEntity)

    // DX Spots
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDxSpots(spots: List<DxSpotEntity>)

    @Query("SELECT * FROM dx_spots_cache ORDER BY timestamp DESC")
    fun getDxSpots(): Flow<List<DxSpotEntity>>

    @Query("DELETE FROM dx_spots_cache WHERE timestamp < :expiry")
    suspend fun cleanOldDxSpots(expiry: Long)

    // APRS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAprsPackets(packets: List<AprsPacketEntity>)

    @Query("SELECT * FROM aprs_cache")
    fun getAprsPackets(): Flow<List<AprsPacketEntity>>

    @Query("DELETE FROM aprs_cache WHERE timestamp < :expiry")
    suspend fun cleanOldAprs(expiry: Long)

    // Favourites
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFreq(freq: FavoriteFrequencyEntity)

    @Delete
    suspend fun deleteFreq(freq: FavoriteFrequencyEntity)

    @Query("SELECT * FROM favorite_frequencies")
    fun getFavoriteFrequencies(): Flow<List<FavoriteFrequencyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSdr(sdr: FavoriteSdrEntity)

    @Delete
    suspend fun deleteSdr(sdr: FavoriteSdrEntity)

    @Query("SELECT * FROM favorite_sdrs")
    fun getFavoriteSdrs(): Flow<List<FavoriteSdrEntity>>

    // Settings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: UserSettingsEntity)

    @Query("SELECT * FROM user_settings WHERE id = 0")
    fun getSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 0")
    fun getSettingsSync(): UserSettingsEntity?

    // Callsign Lookup Cache
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallsign(entry: CallsignCacheEntity)

    @Query("SELECT * FROM callsign_cache WHERE callsign = :callsign LIMIT 1")
    suspend fun getCallsign(callsign: String): CallsignCacheEntity?
}
