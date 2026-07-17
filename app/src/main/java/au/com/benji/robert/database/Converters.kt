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

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return try {
            Json.decodeFromString(value)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun fromQslStatus(value: au.com.benji.robert.models.QslStatus): String {
        return value.name
    }

    @TypeConverter
    fun toQslStatus(value: String): au.com.benji.robert.models.QslStatus {
        return try {
            au.com.benji.robert.models.QslStatus.valueOf(value)
        } catch (_: Exception) {
            au.com.benji.robert.models.QslStatus.NONE
        }
    }
}
