package au.com.benji.robert.models

import kotlinx.serialization.Serializable

@Serializable
data class DetailedWeather(
    val locationName: String = "Unknown Location",
    val temperature: Double = 0.0,
    val unit: String = "°C",
    val condition: String = "Unknown",
    val humidity: Int = 0,
    val windSpeed: Double = 0.0,
    val apparentTemperature: Double = 0.0,
    val sunrise: String = "--:--",
    val sunset: String = "--:--"
)
