package au.com.benji.robert.repository

import au.com.benji.robert.database.CacheDao
import au.com.benji.robert.database.UserSettingsEntity
import kotlinx.coroutines.flow.*

class SettingsRepository(private val cacheDao: CacheDao) {

    val settings: Flow<UserSettingsEntity> = cacheDao.getSettings()
        .map { it ?: UserSettingsEntity() }
        .distinctUntilChanged()

    val callsign: Flow<String> = settings.map { it.callsign }
    val name: Flow<String> = settings.map { it.name }
    val gridSquare: Flow<String> = settings.map { it.gridSquare }
    val country: Flow<String> = settings.map { it.country }
    val licenceClass: Flow<String> = settings.map { it.licenceClass }
    val themeMode: Flow<String> = settings.map { it.theme }
    val kiwisdrUrl: Flow<String> = settings.map { it.kiwisdrUrl }

    suspend fun saveSettings(callsign: String, name: String, gridSquare: String, country: String, licenceClass: String) {
        val current = cacheDao.getSettings().first() ?: UserSettingsEntity()
        cacheDao.updateSettings(current.copy(
            callsign = callsign,
            name = name,
            gridSquare = gridSquare,
            country = country,
            licenceClass = licenceClass
        ))
    }

    suspend fun saveThemeMode(mode: String) {
        val current = cacheDao.getSettings().first() ?: UserSettingsEntity()
        cacheDao.updateSettings(current.copy(theme = mode))
    }

    suspend fun saveKiwisdrUrl(url: String) {
        val current = cacheDao.getSettings().first() ?: UserSettingsEntity()
        cacheDao.updateSettings(current.copy(kiwisdrUrl = url))
    }

    suspend fun updateLocation(lat: Double, lon: Double) {
        val current = cacheDao.getSettings().first() ?: UserSettingsEntity()
        cacheDao.updateSettings(current.copy(homeLat = lat, homeLon = lon))
    }
}
