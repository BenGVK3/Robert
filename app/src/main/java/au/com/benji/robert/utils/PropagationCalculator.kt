package au.com.benji.robert.utils

import au.com.benji.robert.models.SolarData
import au.com.benji.robert.repository.propagation.AuroraReport
import au.com.benji.robert.repository.propagation.ESkipReport
import java.util.Calendar
import kotlin.math.roundToInt

object PropagationCalculator {

    data class BandScore(
        val band: String,
        val score: Int,
        val rating: String,
        val colorHex: String
    )

    fun calculateESkip(
        solarData: SolarData,
        muf: Double,
        psk6mReports: Int = 0,
        psk10mReports: Int = 0
    ): ESkipReport {
        var score = 0.0

        // MUF Score
        score += when {
            muf < 30.0 -> 0
            muf < 40.0 -> 15
            muf < 50.0 -> 35
            muf < 60.0 -> 60
            muf < 70.0 -> 80
            else -> 100
        }

        // Solar Flux Bonus
        val sfi = solarData.solarFlux
        score += when {
            sfi < 80 -> 0
            sfi < 120 -> 5
            sfi < 160 -> 10
            sfi < 200 -> 15
            else -> 20
        }

        // Local Time Bonus
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        score += when (hour) {
            in 6..9 -> 10
            in 10..17 -> 20
            in 18..21 -> 10
            else -> 0
        }

        // Southern Hemisphere Seasonal Bonus
        val month = Calendar.getInstance().get(Calendar.MONTH) // 0-indexed
        score += when (month) {
            Calendar.DECEMBER -> 20
            Calendar.JANUARY -> 15
            Calendar.NOVEMBER, Calendar.FEBRUARY -> 10
            else -> 0
        }

        // PSK Reporter Activity Bonus
        score += when {
            psk6mReports >= 500 -> 30
            psk6mReports >= 200 -> 20
            psk6mReports >= 50 -> 10
            else -> 0
        }
        score += when {
            psk10mReports >= 200 -> 10
            psk10mReports >= 50 -> 5
            else -> 0
        }

        val finalScore = score.roundToInt().coerceIn(0, 100)

        val (status, color, description) = when {
            finalScore >= 90 -> Triple("Extreme", "#F44336", "Major E-Skip event likely. Check 6m, 10m and possibly 2m.")
            finalScore >= 75 -> Triple("High", "#FF9800", "Excellent chance of 6m E-Skip.")
            finalScore >= 60 -> Triple("Moderate", "#4CAF50", "Watch 6m closely.")
            finalScore >= 40 -> Triple("Low", "#00BCD4", "Possible short openings.")
            finalScore >= 20 -> Triple("Very Low", "#2196F3", "Unlikely, monitor 10m and 6m.")
            else -> Triple("None", "#9E9E9E", "No E-Skip expected.")
        }

        return ESkipReport(finalScore, status, color, description)
    }

    fun calculateAurora(solarData: SolarData): AuroraReport {
        val k = solarData.kIndex.toDouble()
        val wind = solarData.solarWind.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 350.0
        
        // Try to extract Bz from magneticField string (e.g. "Bz: -2.4")
        val bz = if (solarData.magneticField.contains("Bz:")) {
            solarData.magneticField.substringAfter("Bz:").trim().split(" ").firstOrNull()?.toDoubleOrNull() ?: 0.0
        } else {
            solarData.magneticField.toDoubleOrNull() ?: 0.0
        }

        var score = 0.0

        // K Index Score
        score += when {
            k < 4.0 -> 0
            k < 5.0 -> 20
            k < 6.0 -> 40
            k < 7.0 -> 60
            k < 8.0 -> 80
            else -> 100
        }

        // Solar Wind Bonus
        score += when {
            wind < 400.0 -> 0
            wind < 500.0 -> 5
            wind < 600.0 -> 10
            wind < 700.0 -> 15
            else -> 20
        }

        // IMF Bz Bonus
        score += when {
            bz > 0.0 -> 0
            bz > -5.0 -> 5
            bz > -10.0 -> 10
            bz > -15.0 -> 15
            else -> 20
        }

        val finalScore = score.roundToInt().coerceIn(0, 100)

        val (status, color, description) = when {
            finalScore >= 90 -> Triple("Extreme", "#F44336", "Outstanding 6m and 2m auroral propagation.")
            finalScore >= 75 -> Triple("High", "#FF9800", "Excellent 6m, good 2m aurora.")
            finalScore >= 60 -> Triple("Moderate", "#4CAF50", "Good chance on 6m, slight chance on 2m.")
            finalScore >= 40 -> Triple("Low", "#00BCD4", "Possible weak 6m aurora.")
            finalScore >= 20 -> Triple("Very Low", "#2196F3", "Slight chance on 6m.")
            else -> Triple("None", "#9E9E9E", "No auroral propagation expected.")
        }

        return AuroraReport(finalScore, status, color, description)
    }

    fun calculateAllBands(
        solarData: SolarData,
        muf: Double,
        lat: Double?,
        lon: Double?,
        sunrise: String?, // HH:mm
        sunset: String?   // HH:mm
    ): List<BandScore> {
        val sfi = solarData.solarFlux
        val k = solarData.kIndex
        val a = solarData.aIndex
        val isDay = isDaytime(lat, lon, sunrise, sunset)
        val isGoldenHour = isNearSunriseOrSunset(sunrise, sunset)
        val sporadicE = solarData.eSkip.contains("Strong") || solarData.eSkip.contains("Moderate")

        return listOf(
            calculate160m(isDay, k, a, sfi, muf),
            calculate80m(isDay, k, a, sfi),
            calculate40m(isDay, isGoldenHour, k, muf),
            calculate30m(sfi, muf, k, a, isDay),
            calculate20m(sfi, muf, k, a, isDay),
            calculate17m(sfi, muf, k, isDay),
            calculate15m(sfi, muf, k),
            calculate12m(sfi, muf, k),
            calculate10m(sfi, muf, k, isDay),
            calculate6m(muf, sporadicE, k)
        )
    }

    private fun clamp(score: Double): Int = score.roundToInt().coerceIn(0, 100)

    fun getRating(score: Int): String = when {
        score >= 90 -> "Excellent"
        score >= 75 -> "Very Good"
        score >= 60 -> "Good"
        score >= 40 -> "Fair"
        score >= 20 -> "Poor"
        else -> "Closed"
    }

    fun getColor(score: Int): String = when {
        score >= 90 -> "#00FF00" // Bright Green
        score >= 75 -> "#4CAF50" // Green
        score >= 60 -> "#CDDC39" // Lime
        score >= 40 -> "#FFEB3B" // Yellow
        score >= 20 -> "#FF9800" // Orange
        else -> "#F44336" // Red/Grey
    }

    private fun calculate160m(isDay: Boolean, k: Int, a: Int, sfi: Int, muf: Double): BandScore {
        var score = 50.0
        score += if (!isDay) 25 else -30
        score -= (k * 6)
        score -= (a * 0.5)
        if (sfi < 140) score += 8
        if (sfi > 180) score -= 5
        if (muf < 12) score += 10
        if (muf > 18) score -= 10
        
        val finalScore = clamp(score)
        return BandScore("160m", finalScore, getRating(finalScore), getColor(finalScore))
    }

    private fun calculate80m(isDay: Boolean, k: Int, a: Int, sfi: Int): BandScore {
        var score = 55.0
        score += if (!isDay) 20 else -20
        score -= (k * 5)
        score -= (a * 0.4)
        if (sfi < 170) score += 5
        
        val finalScore = clamp(score)
        return BandScore("80m", finalScore, getRating(finalScore), getColor(finalScore))
    }

    private fun calculate40m(isDay: Boolean, isGoldenHour: Boolean, k: Int, muf: Double): BandScore {
        var score = 60.0
        if (isGoldenHour) score += 20
        if (!isDay) score += 10
        score -= (k * 4)
        if (muf > 10) score += 5
        
        val finalScore = clamp(score)
        return BandScore("40m", finalScore, getRating(finalScore), getColor(finalScore))
    }

    private fun calculate30m(sfi: Int, muf: Double, k: Int, a: Int, isDay: Boolean): BandScore {
        var score = 55.0
        score += (sfi * 0.20)
        score += (muf * 1.5)
        score -= (k * 5)
        score -= (a * 0.4)
        if (isDay) score += 10
        
        val finalScore = clamp(score)
        return BandScore("30m", finalScore, getRating(finalScore), getColor(finalScore))
    }

    private fun calculate20m(sfi: Int, muf: Double, k: Int, a: Int, isDay: Boolean): BandScore {
        var score = 40.0
        score += (sfi * 0.30)
        score += (muf * 2.0)
        score -= (k * 6)
        score -= (a * 0.5)
        score += if (isDay) 15 else -15
        
        val finalScore = clamp(score)
        return BandScore("20m", finalScore, getRating(finalScore), getColor(finalScore))
    }

    private fun calculate17m(sfi: Int, muf: Double, k: Int, isDay: Boolean): BandScore {
        var score = 30.0
        score += (sfi * 0.35)
        score += (muf * 2.5)
        score -= (k * 7)
        if (isDay) score += 15
        
        val finalScore = clamp(score)
        return BandScore("17m", finalScore, getRating(finalScore), getColor(finalScore))
    }

    private fun calculate15m(sfi: Int, muf: Double, k: Int): BandScore {
        var score = 20.0
        score += (sfi * 0.45)
        score += (muf * 3.0)
        score -= (k * 8)
        if (sfi > 170) score += 20
        if (sfi < 140) score -= 20
        
        val finalScore = clamp(score)
        return BandScore("15m", finalScore, getRating(finalScore), getColor(finalScore))
    }

    private fun calculate12m(sfi: Int, muf: Double, k: Int): BandScore {
        var score = 10.0
        score += (sfi * 0.50)
        score += (muf * 4.0)
        score -= (k * 8)
        if (muf > 24) score += 25
        
        val finalScore = clamp(score)
        return BandScore("12m", finalScore, getRating(finalScore), getColor(finalScore))
    }

    private fun calculate10m(sfi: Int, muf: Double, k: Int, isDay: Boolean): BandScore {
        var score = 5.0
        score += (sfi * 0.60)
        score += (muf * 5.0)
        score -= (k * 10)
        if (sfi > 180) score += 30
        if (muf > 28) score += 25
        if (!isDay) score -= 30
        
        val finalScore = clamp(score)
        return BandScore("10m", finalScore, getRating(finalScore), getColor(finalScore))
    }

    private fun calculate6m(muf: Double, sporadicE: Boolean, k: Int): BandScore {
        var score = 20.0
        if (muf > 50) score += 40
        if (sporadicE) score += 60
        score -= (k * 2)
        
        val finalScore = clamp(score)
        return BandScore("6m", finalScore, getRating(finalScore), getColor(finalScore))
    }

    private fun isDaytime(lat: Double?, lon: Double?, sunrise: String?, sunset: String?): Boolean {
        if (sunrise != null && sunset != null) {
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            
            val sunriseParts = sunrise.split(":")
            val sunsetParts = sunset.split(":")
            if (sunriseParts.size == 2 && sunsetParts.size == 2) {
                val sunriseMinutes = sunriseParts[0].toInt() * 60 + sunriseParts[1].toInt()
                val sunsetMinutes = sunsetParts[0].toInt() * 60 + sunsetParts[1].toInt()
                return currentMinutes in sunriseMinutes..sunsetMinutes
            }
        }
        
        // Fallback
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour in 7..18
    }

    private fun isNearSunriseOrSunset(sunrise: String?, sunset: String?): Boolean {
        if (sunrise == null || sunset == null) return false
        
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        
        fun getMinutes(time: String): Int {
            val parts = time.split(":")
            return if (parts.size == 2) parts[0].toInt() * 60 + parts[1].toInt() else -1000
        }
        
        val sunriseMinutes = getMinutes(sunrise)
        val sunsetMinutes = getMinutes(sunset)
        
        return Math.abs(currentMinutes - sunriseMinutes) <= 60 || Math.abs(currentMinutes - sunsetMinutes) <= 60
    }
}
