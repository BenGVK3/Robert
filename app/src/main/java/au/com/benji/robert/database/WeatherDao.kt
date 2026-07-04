package au.com.benji.robert.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_data WHERE id = 0")
    fun getWeatherData(): Flow<WeatherEntity?>

    @Query("SELECT * FROM weather_data WHERE id = 0")
    suspend fun getWeatherDataOnce(): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherData(data: WeatherEntity)
}
