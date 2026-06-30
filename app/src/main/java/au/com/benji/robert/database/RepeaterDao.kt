package au.com.benji.robert.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RepeaterDao {
    @Query("SELECT * FROM repeaters")
    fun getAll(): Flow<List<RepeaterEntity>>

    @Query("SELECT * FROM repeaters WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<RepeaterEntity>>

    @Query("SELECT * FROM repeaters WHERE isRecent = 1 ORDER BY lastViewed DESC LIMIT 20")
    fun getRecent(): Flow<List<RepeaterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(repeaters: List<RepeaterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(repeater: RepeaterEntity)

    @Update
    suspend fun update(repeater: RepeaterEntity)

    @Query("UPDATE repeaters SET isFavorite = :isFavorite WHERE callsign = :callsign AND frequency = :frequency")
    suspend fun setFavorite(callsign: String, frequency: String, isFavorite: Boolean)

    @Query("UPDATE repeaters SET isRecent = 1, lastViewed = :timestamp WHERE callsign = :callsign AND frequency = :frequency")
    suspend fun addToRecent(callsign: String, frequency: String, timestamp: Long)

    @Query("SELECT * FROM repeaters WHERE callsign = :callsign AND frequency = :frequency")
    suspend fun getByKeys(callsign: String, frequency: String): RepeaterEntity?
    
    @Query("DELETE FROM repeaters WHERE isFavorite = 0 AND isRecent = 0")
    suspend fun clearCache()

    @Query("DELETE FROM repeaters")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM repeaters")
    suspend fun getCount(): Int
}
