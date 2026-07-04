package au.com.benji.robert.repository

import au.com.benji.robert.database.WeatherDao
import au.com.benji.robert.database.WeatherEntity
import au.com.benji.robert.models.DetailedWeather
import au.com.benji.robert.models.ForecastDay
import au.com.benji.robert.network.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

class WeatherRepository(private val weatherDao: WeatherDao) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getCurrentWeather(lat: Double, lon: Double, locationName: String): Flow<DetailedWeather?> {
        val refreshFlow = flow<Unit?> {
            while (true) {
                try {
                    val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset&timezone=auto"
                    val response = ApiService.fetchData(url)
                    
                    if (response != null) {
                        val root = json.parseToJsonElement(response).jsonObject
                        val current = root["current"]?.jsonObject
                        val currentUnits = root["current_units"]?.jsonObject
                        val daily = root["daily"]?.jsonObject
                        
                        val temp = current?.get("temperature_2m")?.jsonPrimitive?.doubleOrNull ?: 0.0
                        val unit = currentUnits?.get("temperature_2m")?.jsonPrimitive?.content ?: "°C"
                        val humidity = current?.get("relative_humidity_2m")?.jsonPrimitive?.intOrNull ?: 0
                        val windSpeed = current?.get("wind_speed_10m")?.jsonPrimitive?.doubleOrNull ?: 0.0
                        val apparentTemp = current?.get("apparent_temperature")?.jsonPrimitive?.doubleOrNull ?: 0.0
                        val weatherCode = current?.get("weather_code")?.jsonPrimitive?.intOrNull ?: 0
                        
                        val sunriseFull = daily?.get("sunrise")?.jsonArray?.get(0)?.jsonPrimitive?.content ?: ""
                        val sunsetFull = daily?.get("sunset")?.jsonArray?.get(0)?.jsonPrimitive?.content ?: ""
                        
                        val sunrise = if (sunriseFull.length >= 16) sunriseFull.substring(11, 16) else "--:--"
                        val sunset = if (sunsetFull.length >= 16) sunsetFull.substring(11, 16) else "--:--"

                        val forecast = mutableListOf<ForecastDay>()
                        daily?.let { d ->
                            val times = d["time"]?.jsonArray ?: emptyList()
                            val codes = d["weather_code"]?.jsonArray ?: emptyList()
                            val maxTemps = d["temperature_2m_max"]?.jsonArray ?: emptyList()
                            val minTemps = d["temperature_2m_min"]?.jsonArray ?: emptyList()

                            for (i in 0 until times.size) {
                                forecast.add(
                                    ForecastDay(
                                        date = times[i].jsonPrimitive.content,
                                        maxTemp = maxTemps[i].jsonPrimitive.doubleOrNull ?: 0.0,
                                        minTemp = minTemps[i].jsonPrimitive.doubleOrNull ?: 0.0,
                                        condition = getWeatherCondition(codes[i].jsonPrimitive.intOrNull ?: 0),
                                        weatherCode = codes[i].jsonPrimitive.intOrNull ?: 0
                                    )
                                )
                            }
                        }

                        weatherDao.insertWeatherData(WeatherEntity(
                            id = 0,
                            locationName = locationName,
                            temperature = temp,
                            unit = unit,
                            condition = getWeatherCondition(weatherCode),
                            humidity = humidity,
                            windSpeed = windSpeed,
                            apparentTemperature = apparentTemp,
                            sunrise = sunrise,
                            sunset = sunset,
                            lastUpdated = System.currentTimeMillis(),
                            forecast = forecast
                        ))
                        delay(10 * 60 * 1000)
                    } else {
                        delay(60 * 1000)
                    }
                } catch (e: Exception) {
                    delay(60 * 1000)
                }
                emit(null)
            }
        }

        return merge(
            weatherDao.getWeatherData().filterNotNull().map { it.toModel() },
            refreshFlow.filter { false }.map { DetailedWeather("", 0.0, "", "", 0, 0.0, 0.0, "", "", 0, emptyList()) }
        ).distinctUntilChanged()
    }

    private fun WeatherEntity.toModel() = DetailedWeather(
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

    fun getWeatherCondition(code: Int): String {
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
