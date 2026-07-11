package au.com.benji.robert.utils

import au.com.benji.robert.models.SolarData
import java.util.Calendar

object MufCalculator {
    
    enum class Confidence {
        High, Medium, Low
    }
    
    data class MufResult(
        val value: Double,
        val isEstimated: Boolean,
        val confidence: Confidence,
        val foF2: Double = 0.0
    )

    fun calculateMuf(solarData: SolarData, lat: Double? = null, lon: Double? = null): MufResult {
        val reportedMuf = solarData.muf.replace(" MHz", "").toDoubleOrNull()
        val reportedFoF2 = solarData.foF2.replace(" MHz", "").toDoubleOrNull()
        
        if (reportedMuf != null && solarData.muf != "NoRpt" && solarData.muf != "---") {
            return MufResult(reportedMuf, false, Confidence.High, reportedFoF2 ?: (reportedMuf / 3.1))
        }
        
        // Estimate MUF
        // 1. Try foF2 if available
        if (reportedFoF2 != null && reportedFoF2 > 1.0) {
            return MufResult(reportedFoF2 * 3.1, true, Confidence.High, reportedFoF2)
        }
        
        // 2. Use SFI, K-Index and Time of Day
        val sfi = solarData.solarFlux.toDouble().coerceAtLeast(60.0)
        val k = solarData.kIndex.toDouble()
        
        // Determine if it's day or night at location
        val isDay = isDaytime(lat, lon)
        
        var estimatedMuf = if (isDay) {
            // Daytime empirical formula: roughly 0.16 * SFI + 12
            (sfi * 0.16) + 12.0
        } else {
            // Nighttime empirical formula: roughly 0.08 * SFI + 5
            (sfi * 0.08) + 5.0
        }
        
        // Adjust for K-Index (High K = lower MUF due to ionospheric storming)
        // A K of 9 could drop MUF by 40%
        val kFactor = (1.0 - (k * 0.05)).coerceAtLeast(0.4)
        estimatedMuf *= kFactor
        
        // 3. Refine with band conditions
        val conditions = if (isDay) solarData.hfConditionsDay else solarData.hfConditionsNight
        
        val bandLimits = mapOf(
            "10m" to 28.0,
            "12m" to 24.9,
            "15m" to 21.0,
            "17m" to 18.1,
            "20m" to 14.0,
            "30m" to 10.1,
            "40m" to 7.0,
            "80m" to 3.5
        )
        
        var conditionBasedMin = 0.0
        for ((band, limit) in bandLimits) {
            val rating = conditions[band]
            if (rating == "Good" || rating == "Excellent") {
                conditionBasedMin = maxOf(conditionBasedMin, limit)
            }
        }
        
        if (estimatedMuf < conditionBasedMin) {
            estimatedMuf = conditionBasedMin + (Math.random() * 2.0) // Add a small random buffer for realism
        }
        
        // Sanity checks
        if (estimatedMuf < 3.0) estimatedMuf = 3.0
        if (estimatedMuf > 100.0) estimatedMuf = 100.0
        
        val confidence = when {
            k > 4 -> Confidence.Low
            sfi < 70 -> Confidence.Medium
            reportedFoF2 != null && reportedFoF2 > 0 -> Confidence.High
            else -> Confidence.Medium
        }
        
        return MufResult(estimatedMuf, true, confidence, reportedFoF2 ?: (estimatedMuf / 3.1))
    }
    
    private fun isDaytime(lat: Double?, lon: Double?): Boolean {
        if (lat == null || lon == null) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return hour in 7..18
        }
        
        val calendar = Calendar.getInstance()
        val utcHour = calendar.get(Calendar.HOUR_OF_DAY) - (calendar.get(Calendar.ZONE_OFFSET) / 3600000)
        val localSolarHour = (utcHour + (lon / 15.0) + 24) % 24
        
        return localSolarHour in 6.0..18.0
    }
}
