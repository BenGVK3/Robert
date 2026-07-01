package au.com.benji.robert.repository

import au.com.benji.robert.models.MoonData
import au.com.benji.robert.network.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import java.util.*
import kotlin.math.*

class MoonRepository {
    private val json = Json { ignoreUnknownKeys = true }

    fun getMoonData(lat: Double?, lon: Double?): Flow<MoonData> = flow {
        while (true) {
            try {
                // Use Open-Meteo for daily moon rise/set/phase
                val dailyUrl = "https://api.open-meteo.com/v1/forecast?latitude=${lat ?: -37.81}&longitude=${lon ?: 144.96}&daily=moonrise,moonset,moon_phase&timezone=auto"
                val response = ApiService.fetchData(dailyUrl)
                
                var rise = "--:--"
                var set = "--:--"
                var phaseNum = 0.5

                response?.let {
                    val root = json.parseToJsonElement(it).jsonObject
                    val daily = root["daily"]?.jsonObject
                    val moonriseArr = daily?.get("moonrise")?.jsonArray
                    val moonsetArr = daily?.get("moonset")?.jsonArray
                    val phaseArr = daily?.get("moon_phase")?.jsonArray
                    
                    val riseFull = moonriseArr?.get(0)?.jsonPrimitive?.content ?: ""
                    val setFull = moonsetArr?.get(0)?.jsonPrimitive?.content ?: ""
                    phaseNum = phaseArr?.get(0)?.jsonPrimitive?.double ?: 0.5
                    
                    if (riseFull.length >= 16) rise = riseFull.substring(11, 16)
                    if (setFull.length >= 16) set = setFull.substring(11, 16)
                }

                // Real-time calculations for EME
                val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val moonCalc = calculateMoonEME(now, lat ?: -37.81, lon ?: 144.96)

                val data = MoonData(
                    phaseName = getPhaseName(phaseNum),
                    phaseIcon = getPhaseIcon(phaseNum),
                    illumination = (moonCalc.illumination * 100).toInt(),
                    age = phaseNum * 29.53,
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
                    emeRating = if (moonCalc.altitude > 0) "Good" else "Poor"
                )
                
                emit(data)
            } catch (e: Exception) {
                // Fallback or retry
            }
            delay(60 * 1000) // Update every minute
        }
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
        // Doppler = freq * v / c
        return (freqMhz * 1e6 * velocityMs / 299792458.0)
    }

    private fun calculatePathLoss(freqMhz: Double, distKm: Double): Double {
        // Free space path loss = 20 log10(d) + 20 log10(f) + 32.44
        return 20 * log10(distKm) + 20 * log10(freqMhz) + 32.44 + 2.0 // +2dB for moon surface
    }

    data class EmeResult(
        val altitude: Double,
        val azimuth: Double,
        val distance: Double,
        val declination: Double,
        val illumination: Double,
        val radialVelocity: Double
    )

    private fun calculateMoonEME(cal: Calendar, lat: Double, lon: Double): EmeResult {
        // This is a simplified version of the low-precision moon formulas
        val d = (cal.timeInMillis / 86400000.0) + 2440587.5 - 2451545.0
        
        val L = normalize(218.316 + 13.176396 * d)
        val M = normalize(134.963 + 13.064993 * d)
        val F = normalize(93.272 + 13.229350 * d)
        
        val long = L + 6.289 * sin(Math.toRadians(M))
        val lat_moon = 5.128 * sin(Math.toRadians(F))
        val dist = 385001 - 20905 * cos(Math.toRadians(M))
        
        val epsilon = 23.439 - 0.0000004 * d
        val ra = Math.toDegrees(atan2(sin(Math.toRadians(long)) * cos(Math.toRadians(epsilon)) - tan(Math.toRadians(lat_moon)) * sin(Math.toRadians(epsilon)), cos(Math.toRadians(long))))
        val dec = Math.toDegrees(asin(sin(Math.toRadians(lat_moon)) * cos(Math.toRadians(epsilon)) + cos(Math.toRadians(lat_moon)) * sin(Math.toRadians(epsilon)) * sin(Math.toRadians(long))))
        
        // Local Sidereal Time
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
        
        // Approx radial velocity (very rough estimate for doppler)
        val radialVel = 100 * sin(Math.toRadians(M)) // m/s
        
        return EmeResult(alt, az, dist, dec, (1 + cos(Math.toRadians(long - L))) / 2, radialVel)
    }

    private fun normalize(v: Double): Double {
        var res = v % 360
        if (res < 0) res += 360
        return res
    }
}
