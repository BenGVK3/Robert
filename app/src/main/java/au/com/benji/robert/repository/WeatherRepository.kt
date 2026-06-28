package au.com.benji.robert.repository

import au.com.benji.robert.models.DetailedWeather
import au.com.benji.robert.network.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*

class WeatherRepository {
    private val json = Json { ignoreUnknownKeys = true }

    fun getCurrentWeather(lat: Double, lon: Double, locationName: String): Flow<DetailedWeather?> = flow {
        while (true) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m"
                val response = ApiService.fetchData(url)
                
                val weather = response?.let {
                    val root = json.parseToJsonElement(it).jsonObject
                    val current = root["current"]?.jsonObject
                    val currentUnits = root["current_units"]?.jsonObject
                    
                    val temp = current?.get("temperature_2m")?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val unit = currentUnits?.get("temperature_2m")?.jsonPrimitive?.content ?: "°C"
                    val humidity = current?.get("relative_humidity_2m")?.jsonPrimitive?.intOrNull ?: 0
                    val windSpeed = current?.get("wind_speed_10m")?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val apparentTemp = current?.get("apparent_temperature")?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val weatherCode = current?.get("weather_code")?.jsonPrimitive?.intOrNull ?: 0
                    
                    DetailedWeather(
                        locationName = locationName,
                        temperature = temp,
                        unit = unit,
                        condition = getWeatherCondition(weatherCode),
                        humidity = humidity,
                        windSpeed = windSpeed,
                        apparentTemperature = apparentTemp
                    )
                }
                
                emit(weather)
            } catch (e: Exception) {
                emit(null)
            }
            delay(30 * 60 * 1000) // 30 minutes
        }
    }

    private fun getWeatherCondition(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Mainly clear"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            80, 81, 82 -> "Rain showers"
            95 -> "Thunderstorm"
            else -> "Unknown"
        }
    }
}
