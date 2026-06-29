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
    }

    val callsign: Flow<String> = context.dataStore.data.map { it[CALLSIGN] ?: "VK3XYZ" }
    val name: Flow<String> = context.dataStore.data.map { it[NAME] ?: "Benji" }
    val gridSquare: Flow<String> = context.dataStore.data.map { it[GRID_SQUARE] ?: "QF22og" }
    val country: Flow<String> = context.dataStore.data.map { it[COUNTRY] ?: "Australia" }
    val licenceClass: Flow<String> = context.dataStore.data.map { it[LICENCE_CLASS] ?: "foundation" }

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
