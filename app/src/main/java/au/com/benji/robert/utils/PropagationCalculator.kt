package au.com.benji.robert.utils

import au.com.benji.robert.models.SolarData
import au.com.benji.robert.repository.propagation.AuroraReport
import au.com.benji.robert.repository.propagation.ESkipReport
import au.com.benji.robert.repository.propagation.OperatingSummary
import java.util.Calendar
import kotlin.math.exp
import kotlin.math.roundToInt

object PropagationCalculator {

    data class BandScore(
        val band: String,
        val score: Int,
        val rating: String,
        val colorHex: String,
        val summaries: List<OperatingSummary> = emptyList()
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
        sunrise: String?,
        sunset: String?
    ): List<BandScore> {
        val sfi = solarData.solarFlux
        val k = solarData.kIndex
        val a = solarData.aIndex
        val isDay = isDaytime(lat, lon, sunrise, sunset)
        val sporadicE = solarData.eSkip.contains("Strong") || solarData.eSkip.contains("Moderate")

        val bands = listOf(
            BandInfo("160m", 1.8),
            BandInfo("80m", 3.5),
            BandInfo("40m", 7.1),
            BandInfo("30m", 10.1),
            BandInfo("20m", 14.2),
            BandInfo("17m", 18.1),
            BandInfo("15m", 21.2),
            BandInfo("12m", 24.9),
            BandInfo("10m", 28.5),
            BandInfo("6m", 50.1)
        )

        return bands.map { info ->
            calculateScoreForBand(info, sfi, muf, k, a, isDay, sporadicE)
        }
    }

    private data class BandInfo(val name: String, val centerFreq: Double)

    private fun calculateScoreForBand(
        info: BandInfo,
        sfi: Int,
        muf: Double,
        k: Int,
        a: Int,
        isDay: Boolean,
        sporadicE: Boolean
    ): BandScore {
        // 1. SFI Component (35%) - Normalizing to a more realistic range
        // SFI rarely goes below 65. 150+ is excellent. 250+ is historic.
        val sfiScore = ((sfi.toDouble() - 65.0) / 120.0 * 100.0).coerceIn(10.0, 100.0)
        
        // 2. MUF Component (35%)
        // Sigmoid function to create a realistic drop-off when MUF is below band
        val mufRatio = muf / info.centerFreq
        val mufScore = (100.0 / (1.0 + exp(-12.0 * (mufRatio - 0.85)))).coerceIn(0.0, 100.0)
        
        // 3. K-Index Component (15%) - Impact increases with frequency generally
        val kImpact = if (info.centerFreq < 10.0) (k * 6.0) else (k * 10.0)
        val kScore = (100.0 - kImpact).coerceIn(5.0, 100.0)
        
        // 4. A-Index Component (10%)
        val aScore = (100.0 - (a * 1.5)).coerceIn(5.0, 100.0)
        
        // 5. Day/Night Component (5%)
        val isLowBand = info.centerFreq < 10.0
        val dayNightScore = if (isLowBand) {
            if (!isDay) 100.0 else 30.0
        } else {
            if (isDay) 100.0 else 40.0
        }

        // Weighted Average
        var baseScore = (sfiScore * 0.35) + (mufScore * 0.35) + (kScore * 0.15) + (aScore * 0.10) + (dayNightScore * 0.05)

        // Band Specific Refinements
        baseScore = adjustBandSpecifics(info, baseScore, isDay, muf, sfi, sporadicE)

        val finalScore = baseScore.roundToInt().coerceIn(0, 100)
        val rating = getRating(finalScore)
        val color = getColor(finalScore)
        
        val summaries = calculateSummaries(info, sfi, muf, k, a, isDay, sporadicE)

        return BandScore(info.name, finalScore, rating, color, summaries)
    }

    private fun adjustBandSpecifics(info: BandInfo, score: Double, isDay: Boolean, muf: Double, sfi: Int, sporadicE: Boolean): Double {
        var adjusted = score
        val mufRatio = muf / info.centerFreq

        when (info.name) {
            "160m" -> {
                if (isDay) adjusted *= 0.15 // D-layer is very effective
                else adjusted *= 1.3
            }
            "80m" -> {
                if (isDay) adjusted *= 0.25
                else adjusted *= 1.2
            }
            "40m" -> {
                if (isDay) adjusted *= 0.6
                else adjusted *= 1.25
            }
            "15m" -> {
                if (mufRatio < 0.9) adjusted *= 0.7
            }
            "12m" -> {
                if (mufRatio < 0.9) adjusted *= 0.6
            }
            "10m" -> {
                if (mufRatio < 0.85) adjusted *= 0.5
                if (sporadicE) adjusted = maxOf(adjusted, 75.0)
            }
            "6m" -> {
                adjusted = if (muf > 48.0) adjusted else (if (sporadicE) 70.0 else 5.0)
            }
        }
        
        // Bonus for being near the MUF (best propagation often just below MUF)
        if (mufRatio in 0.85..1.15) {
            adjusted *= 1.15
        }
        
        return adjusted.coerceIn(0.0, 100.0)
    }

    private fun calculateSummaries(
        info: BandInfo,
        sfi: Int,
        muf: Double,
        k: Int,
        a: Int,
        isDay: Boolean,
        sporadicE: Boolean
    ): List<OperatingSummary> {
        val summaries = mutableListOf<OperatingSummary>()
        
        val isLowBand = info.centerFreq < 10.0
        val mufRatio = muf / info.centerFreq
        
        // 1. Long-haul DX
        val dxScore = when {
            k > 4 || a > 20 -> 10.0
            info.name == "6m" -> if (muf > 48.0 || sporadicE) 60.0 else 5.0
            info.name == "160m" && isDay -> 0.0
            info.name == "160m" && !isDay -> scoreComponent(sfi.toDouble(), 70, 200) * 0.3 + 60.0
            isLowBand && isDay -> 15.0
            isLowBand && !isDay -> scoreComponent(sfi.toDouble(), 70, 200) * 0.4 + 50.0
            else -> (scoreComponent(sfi.toDouble(), 80, 250) * 0.5 + scoreComponent(mufRatio, 0.9, 1.3) * 0.5)
        }
        summaries.add(OperatingSummary("🌍 Long-haul DX", getRating(dxScore.toInt())))

        // 2. Regional / Local
        val localScore = when {
            isLowBand && isDay -> 70.0 - (k * 5)
            isLowBand && !isDay -> 95.0 - (k * 3)
            !isLowBand && isDay -> 60.0 * mufRatio.coerceAtMost(1.0)
            else -> 20.0
        }
        val localLabel = if (isLowBand) "🏠 NVIS / Local" else "🇦🇺 Australia-wide"
        summaries.add(OperatingSummary(localLabel, getRating(localScore.toInt())))

        // 3. Digital (FT8/FT4) - More resilient to noise
        val digiScore = (dxScore * 1.2).coerceAtMost(100.0)
        summaries.add(OperatingSummary("📡 Digital (FT8/FT4)", getRating(digiScore.toInt())))
        
        // 4. SSB - Needs better SNR
        val ssbScore = (dxScore * 0.8).coerceAtMost(100.0)
        summaries.add(OperatingSummary("🎙️ SSB", getRating(ssbScore.toInt())))

        // 5. CW - Middle ground
        val cwScore = (dxScore * 1.0).coerceAtMost(100.0)
        summaries.add(OperatingSummary("⚡ CW", getRating(cwScore.toInt())))

        return summaries
    }

    private fun scoreComponent(value: Double, min: Number, max: Number): Double {
        return ((value - min.toDouble()) / (max.toDouble() - min.toDouble()) * 100.0).coerceIn(0.0, 100.0)
    }

    fun getRating(score: Int): String = when {
        score >= 90 -> "Excellent"
        score >= 75 -> "Very Good"
        score >= 60 -> "Good"
        score >= 40 -> "Fair"
        score >= 20 -> "Poor"
        else -> "Closed"
    }

    fun getColor(score: Int): String = when {
        score >= 90 -> "#00FF00" 
        score >= 75 -> "#4CAF50" 
        score >= 60 -> "#CDDC39" 
        score >= 40 -> "#FFEB3B" 
        score >= 20 -> "#FF9800" 
        else -> "#F44336" 
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
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour in 7..18
    }
}
