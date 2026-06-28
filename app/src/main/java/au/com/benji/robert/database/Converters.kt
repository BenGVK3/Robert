package au.com.benji.robert.database

import androidx.room.TypeConverter

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
}