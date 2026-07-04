package au.com.benji.robert.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PropagationDao {
    @Insert
    suspend fun insert(entry: PropagationHistoryEntity)

    @Query("SELECT * FROM propagation_history WHERE band = :band AND timestamp > :since ORDER BY timestamp ASC")
    suspend fun getHistoryForBand(band: String, since: Long): List<PropagationHistoryEntity>

    @Query("DELETE FROM propagation_history WHERE timestamp < :before")
    suspend fun deleteOldHistory(before: Long)
}
