package au.com.benji.robert.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PropagationDao {
    @Query("SELECT * FROM propagation_spots ORDER BY timestamp DESC")
    fun getAllSpots(): Flow<List<PropagationSpotEntity>>

    @Query("SELECT * FROM propagation_spots WHERE band = :band AND timestamp >= :minTimestamp ORDER BY timestamp DESC")
    fun getSpotsByBand(band: String, minTimestamp: Long): Flow<List<PropagationSpotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpots(spots: List<PropagationSpotEntity>)

    @Query("DELETE FROM propagation_spots WHERE timestamp < :threshold")
    suspend fun clearOldSpots(threshold: Long)

    @Query("DELETE FROM propagation_spots")
    suspend fun deleteAll()
}
