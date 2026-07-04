package au.com.benji.robert.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import au.com.benji.robert.models.DetailedWeather
import au.com.benji.robert.models.ForecastDay

@Entity(tableName = "weather_data")
data class WeatherEntity(
    @PrimaryKey val id: Int = 0,
    val locationName: String,
    val temperature: Double,
    val unit: String,
    val condition: String,
    val humidity: Int,
    val windSpeed: Double,
    val apparentTemperature: Double,
    val sunrise: String,
    val sunset: String,
    val lastUpdated: Long,
    val forecast: List<ForecastDay>
)

fun DetailedWeather.toEntity(lastUpdated: Long) = WeatherEntity(
    locationName = locationName,
    temperature = temperature,
    unit = unit,
    condition = condition,
    humidity = humidity,
    windSpeed = windSpeed,
    apparentTemperature = apparentTemperature,
    sunrise = sunrise,
    sunset = sunset,
    lastUpdated = lastUpdated,
    forecast = forecast
)

fun WeatherEntity.toDomain() = DetailedWeather(
    locationName = locationName,
    temperature = temperature,
    unit = unit,
    condition = condition,
    humidity = humidity,
    windSpeed = windSpeed,
    apparentTemperature = apparentTemperature,
    sunrise = sunrise,
    sunset = sunset,
    lastUpdated = lastUpdated,
    forecast = forecast
)
