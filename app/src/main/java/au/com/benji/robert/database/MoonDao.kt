package au.com.benji.robert.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MoonDao {
    @Query("SELECT * FROM moon_data WHERE id = 0")
    fun getMoonData(): Flow<MoonDataEntity?>

    @Query("SELECT * FROM moon_data WHERE id = 0")
    suspend fun getMoonDataOnce(): MoonDataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoonData(data: MoonDataEntity)
}
