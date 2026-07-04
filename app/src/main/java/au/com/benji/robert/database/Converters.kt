package au.com.benji.robert.database

import androidx.room.TypeConverter
import au.com.benji.robert.models.ForecastDay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class Converters {

    @TypeConverter
    fun fromString(value: String?): List<String> {

        return value
            ?.split("|")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    @TypeConverter
    fun toString(value: List<String>): String {

        return value.joinToString("|")
    }

    @TypeConverter
    fun fromForecastList(value: List<ForecastDay>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toForecastList(value: String): List<ForecastDay> {
        return try {
            Json.decodeFromString(value)
        } catch (_: Exception) {
            emptyList()
        }
    }
}