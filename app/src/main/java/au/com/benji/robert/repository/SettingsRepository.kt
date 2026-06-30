package au.com.benji.robert.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val CALLSIGN = stringPreferencesKey("callsign")
        val NAME = stringPreferencesKey("name")
        val GRID_SQUARE = stringPreferencesKey("grid_square")
        val COUNTRY = stringPreferencesKey("country")
        val LICENCE_CLASS = stringPreferencesKey("licence_class")
        val KIWISDR_URL = stringPreferencesKey("kiwisdr_url")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val callsign: Flow<String> = context.dataStore.data.map { it[CALLSIGN] ?: "VK3XYZ" }
    val name: Flow<String> = context.dataStore.data.map { it[NAME] ?: "Benji" }
    val gridSquare: Flow<String> = context.dataStore.data.map { it[GRID_SQUARE] ?: "QF22og" }
    val country: Flow<String> = context.dataStore.data.map { it[COUNTRY] ?: "Australia" }
    val licenceClass: Flow<String> = context.dataStore.data.map { it[LICENCE_CLASS] ?: "foundation" }
    val kiwisdrUrl: Flow<String> = context.dataStore.data.map { it[KIWISDR_URL] ?: "https://kiwisdr.com/public/" }
    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "System" }

    suspend fun saveKiwisdrUrl(url: String) {
        context.dataStore.edit { settings ->
            settings[KIWISDR_URL] = url
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { settings ->
            settings[THEME_MODE] = mode
        }
    }

    suspend fun saveSettings(callsign: String, name: String, gridSquare: String, country: String, licenceClass: String) {
        context.dataStore.edit { settings ->
            settings[CALLSIGN] = callsign
            settings[NAME] = name
            settings[GRID_SQUARE] = gridSquare
            settings[COUNTRY] = country
            settings[LICENCE_CLASS] = licenceClass
        }
    }
}
