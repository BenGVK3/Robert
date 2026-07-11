package au.com.benji.robert.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NetDao {
    @Query("SELECT * FROM club_nets ORDER BY time ASC")
    fun getAllNets(): Flow<List<NetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNet(net: NetEntity)

    @Update
    suspend fun updateNet(net: NetEntity)

    @Delete
    suspend fun deleteNet(net: NetEntity)

    @Query("SELECT * FROM club_nets WHERE id = :id")
    suspend fun getNetById(id: Int): NetEntity?
}
