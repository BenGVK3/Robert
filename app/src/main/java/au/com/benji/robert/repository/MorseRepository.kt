package au.com.benji.robert.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import au.com.benji.robert.screens.morse.MorseProgress
import au.com.benji.robert.screens.morse.MorseSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "morse_prefs")

class MorseRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val SETTINGS_KEY = stringPreferencesKey("morse_settings")
        private val PROGRESS_KEY = stringPreferencesKey("morse_progress")
    }

    val settings: Flow<MorseSettings> = context.dataStore.data.map { prefs ->
        prefs[SETTINGS_KEY]?.let {
            try { json.decodeFromString<MorseSettings>(it) } catch (e: Exception) { MorseSettings() }
        } ?: MorseSettings()
    }

    val progress: Flow<MorseProgress> = context.dataStore.data.map { prefs ->
        prefs[PROGRESS_KEY]?.let {
            try { json.decodeFromString<MorseProgress>(it) } catch (e: Exception) { MorseProgress() }
        } ?: MorseProgress()
    }

    suspend fun saveSettings(settings: MorseSettings) {
        context.dataStore.edit { prefs ->
            prefs[SETTINGS_KEY] = json.encodeToString(settings)
        }
    }

    suspend fun saveProgress(progress: MorseProgress) {
        context.dataStore.edit { prefs ->
            prefs[PROGRESS_KEY] = json.encodeToString(progress)
        }
    }
}
