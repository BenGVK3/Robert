package au.com.benji.robert.repository

import au.com.benji.robert.database.MoonDao
import au.com.benji.robert.database.toDomain
import au.com.benji.robert.database.toEntity
import au.com.benji.robert.models.MoonData
import au.com.benji.robert.network.ApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.util.*
import kotlin.math.*

class MoonRepository(private val moonDao: MoonDao) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getMoonData(lat: Double?, lon: Double?): Flow<MoonData> = flow {
        // 1. Emit cached data immediately if available
        val cached = moonDao.getMoonDataOnce()
        if (cached != null) {
            emit(cached.toDomain())
        }

        // 2. Start refresh loop
        while (true) {
            val lastCached = moonDao.getMoonDataOnce()
            val currentTime = System.currentTimeMillis()
            
            // Only refresh if cache is older than 5 minutes or doesn't exist
            if (lastCached == null || (currentTime - lastCached.lastUpdated) > 5 * 60 * 1000) {
                try {
                    val data = fetchAndCalculateMoonData(lat, lon)
                    moonDao.insertMoonData(data.toEntity(currentTime))
                    emit(data.copy(lastUpdated = currentTime))
                } catch (e: Exception) {
                    // If refresh fails, keep using cache but maybe log error
                }
            } else if (lastCached != null) {
                // If cache is fresh, still emit it (in case it wasn't the first emission)
                emit(lastCached.toDomain())
            }
            
            delay(60 * 1000) // Check every minute, but will only fetch if 5 min passed
        }
    }

    private suspend fun fetchAndCalculateMoonData(lat: Double?, lon: Double?): MoonData = coroutineScope {
        // Run API call in parallel using async
        val dailyUrl = "https://api.open-meteo.com/v1/forecast?latitude=${lat ?: -37.81}&longitude=${lon ?: 144.96}&daily=moonrise,moonset,moon_phase&timezone=auto"
        val apiResponseDeferred = async { ApiService.fetchData(dailyUrl) }
        
        // While API is fetching, we could do other things, but here we wait
        val response = apiResponseDeferred.await()
        
        var rise = "--:--"
        var set = "--:--"

        response?.let {
            val root = json.parseToJsonElement(it).jsonObject
            val daily = root["daily"]?.jsonObject
            val moonriseArr = daily?.get("moonrise")?.jsonArray
            val moonsetArr = daily?.get("moonset")?.jsonArray
            
            val riseFull = moonriseArr?.get(0)?.jsonPrimitive?.content ?: ""
            val setFull = moonsetArr?.get(0)?.jsonPrimitive?.content ?: ""
            
            if (riseFull.length >= 16) rise = riseFull.substring(11, 16)
            if (setFull.length >= 16) set = setFull.substring(11, 16)
        }

        // Real-time calculations for EME
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val moonCalc = calculateMoonEME(now, lat ?: -37.81, lon ?: 144.96)

        // Use calculated phase for real-time accuracy, falling back to API if needed
        val livePhase = moonCalc.phase

        MoonData(
            phaseName = getPhaseName(livePhase),
            phaseIcon = getPhaseIcon(livePhase),
            illumination = (moonCalc.illumination * 100).toInt(),
            age = livePhase * 29.53,
            distanceKm = moonCalc.distance,
            angularSize = 0.5, // Approx degrees
            constellation = "Unknown",
            altitude = moonCalc.altitude,
            azimuth = moonCalc.azimuth,
            riseTime = rise,
            setTime = set,
            isVisible = moonCalc.altitude > 0,
            declination = moonCalc.declination,
            radialVelocity = moonCalc.radialVelocity,
            doppler144 = calculateDoppler(144.0, moonCalc.radialVelocity),
            doppler432 = calculateDoppler(432.0, moonCalc.radialVelocity),
            doppler1296 = calculateDoppler(1296.0, moonCalc.radialVelocity),
            pathLoss = calculatePathLoss(432.0, moonCalc.distance),
            oneWayDelay = moonCalc.distance / 299792.458,
            roundTripDelay = (moonCalc.distance * 2) / 299792.458,
            nextFullMoon = getNextPhaseDate(livePhase, 0.5),
            nextNewMoon = getNextPhaseDate(livePhase, 0.0),
            nextFirstQuarter = getNextPhaseDate(livePhase, 0.25),
            nextLastQuarter = getNextPhaseDate(livePhase, 0.75),
            emeRating = if (moonCalc.altitude > 0) "Good" else "Poor"
        )
    }

    private fun getNextPhaseDate(currentPhase: Double, targetPhase: Double): String {
        var diff = targetPhase - currentPhase
        if (diff <= 0) diff += 1.0
        val daysUntil = diff * 29.53
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, (daysUntil * 86400).toInt())
        val sdf = java.text.SimpleDateFormat("MMM dd", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun getPhaseName(phase: Double): String {
        return when {
            phase < 0.06 -> "New Moon"
            phase < 0.25 -> "Waxing Crescent"
            phase < 0.31 -> "First Quarter"
            phase < 0.5 -> "Waxing Gibbous"
            phase < 0.56 -> "Full Moon"
            phase < 0.75 -> "Waning Gibbous"
            phase < 0.81 -> "Last Quarter"
            else -> "Waning Crescent"
        }
    }

    private fun getPhaseIcon(phase: Double): String {
        return when {
            phase < 0.06 -> "🌑"
            phase < 0.25 -> "🌒"
            phase < 0.31 -> "🌓"
            phase < 0.5 -> "🌔"
            phase < 0.56 -> "🌕"
            phase < 0.75 -> "🌖"
            phase < 0.81 -> "🌗"
            else -> "🌘"
        }
    }

    private fun calculateDoppler(freqMhz: Double, velocityMs: Double): Double {
        return (freqMhz * 1e6 * velocityMs / 299792458.0)
    }

    private fun calculatePathLoss(freqMhz: Double, distKm: Double): Double {
        return 20 * log10(distKm) + 20 * log10(freqMhz) + 32.44 + 2.0
    }

    data class EmeResult(
        val altitude: Double,
        val azimuth: Double,
        val distance: Double,
        val declination: Double,
        val illumination: Double,
        val phase: Double,
        val radialVelocity: Double
    )

    private fun calculateMoonEME(cal: Calendar, lat: Double, lon: Double): EmeResult {
        val d = (cal.timeInMillis / 86400000.0) + 2440587.5 - 2451545.0
        
        // Moon Mean Elements
        val L = normalize(218.316 + 13.176396 * d)
        val M = normalize(134.963 + 13.064993 * d)
        val F = normalize(93.272 + 13.229350 * d)
        
        // Moon Geocentric Longitude
        val longMoon = normalize(L + 6.289 * sin(Math.toRadians(M)))
        val latMoon = 5.128 * sin(Math.toRadians(F))
        val dist = 385001 - 20905 * cos(Math.toRadians(M))
        
        // Sun Mean Elements
        val Ms = normalize(357.529 + 0.9856003 * d)
        val Ls = normalize(280.466 + 0.9856474 * d)
        
        // Sun Geocentric Longitude
        val longSun = normalize(Ls + 1.915 * sin(Math.toRadians(Ms)))
        
        // Phase and Illumination
        val D = normalize(longMoon - longSun) // Mean elongation
        val phase = D / 360.0
        // Illumination is (1 - cos(D)) / 2
        val illumination = (1 - cos(Math.toRadians(D))) / 2.0
        
        val epsilon = 23.439 - 0.0000004 * d
        val ra = Math.toDegrees(atan2(sin(Math.toRadians(longMoon)) * cos(Math.toRadians(epsilon)) - tan(Math.toRadians(latMoon)) * sin(Math.toRadians(epsilon)), cos(Math.toRadians(longMoon))))
        val dec = Math.toDegrees(asin(sin(Math.toRadians(latMoon)) * cos(Math.toRadians(epsilon)) + cos(Math.toRadians(latMoon)) * sin(Math.toRadians(epsilon)) * sin(Math.toRadians(longMoon))))
        
        val hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE)/60.0 + cal.get(Calendar.SECOND)/3600.0
        val lst = normalize(100.46 + 0.985647 * d + lon + 15 * hour)
        val ha = normalize(lst - ra)
        
        val x = cos(Math.toRadians(ha)) * cos(Math.toRadians(dec))
        val y = sin(Math.toRadians(ha)) * cos(Math.toRadians(dec))
        val z = sin(Math.toRadians(dec))
        
        val xhor = x * sin(Math.toRadians(lat)) - z * cos(Math.toRadians(lat))
        val yhor = y
        val zhor = x * cos(Math.toRadians(lat)) + z * sin(Math.toRadians(lat))
        
        val az = Math.toDegrees(atan2(yhor, xhor)) + 180
        val alt = Math.toDegrees(asin(zhor))
        
        // Radial velocity approximation (m/s)
        val radialVel = 55.16 * sin(Math.toRadians(M))
        
        return EmeResult(alt, az, dist, dec, illumination, phase, radialVel)
    }

    private fun normalize(v: Double): Double {
        var res = v % 360
        if (res < 0) res += 360
        return res
    }
}
