package au.com.benji.robert.repository.propagation

import au.com.benji.robert.models.SolarData
import kotlin.math.*

class LowBandModel(override val bandName: String, override val centerFreq: Double) : BandModel {
    override fun calculateScore(solarData: SolarData, muf: Double, solarElevation: Double, isGreyline: Boolean, activityReports: Int): Int {
        val sfi = solarData.solarFlux.toDouble()
        val k = solarData.kIndex.toDouble()
        
        // 1. D-Layer Absorption (Huge for low bands)
        val absorption = calculateDLayerAbsorption(centerFreq, solarElevation, solarData.solarFlux)
        val absorptionScore = (100.0 - (absorption * 2.0)).coerceIn(0.0, 100.0)
        
        // 2. Solar Flux (Low impact on propagation, but higher SFI usually means higher noise)
        val sfiScore = if (sfi < 150) 80.0 else 60.0 
        
        // 3. Geomagnetic (High impact)
        val kPenalty = if (bandName == "160m") k * 12.0 else k * 8.0
        val geoScore = (100.0 - kPenalty).coerceIn(0.0, 100.0)
        
        // 4. Greyline / Night Bonus
        val timeBonus = when {
            isGreyline -> 40.0
            solarElevation < -12.0 -> 30.0 // Full night
            else -> 0.0
        }
        
        // 5. Activity
        val activityBonus = (activityReports.toDouble() / 10.0).coerceAtMost(10.0)

        val total = (absorptionScore * 0.5) + (geoScore * 0.3) + (sfiScore * 0.1) + timeBonus + activityBonus
        return total.toInt().coerceIn(5, 100)
    }

    override fun getAtmosphericNoise(solarElevation: Double): Double {
        // Night is noisier on low bands due to lack of D-layer attenuation of distant storms
        return if (solarElevation < 0) 70.0 else 40.0
    }
}

class MidBandModel(override val bandName: String, override val centerFreq: Double) : BandModel {
    override fun calculateScore(solarData: SolarData, muf: Double, solarElevation: Double, isGreyline: Boolean, activityReports: Int): Int {
        // 20m/30m/40m - The workhorses
        val sfiScore = ((solarData.solarFlux - 65.0) / 1.5).coerceIn(0.0, 100.0)
        val mufFactor = calculateMufFactor(centerFreq, muf)
        val geoScore = (100.0 - (solarData.kIndex * 7.0)).coerceIn(0.0, 100.0)
        
        val greylineBonus = if (isGreyline) 25.0 else 0.0
        val activityBonus = (activityReports.toDouble() / 20.0).coerceAtMost(15.0)

        // Mid bands handle D-layer better than low bands but still affected
        val absorption = calculateDLayerAbsorption(centerFreq, solarElevation, solarData.solarFlux)
        val absorptionFactor = (100.0 - absorption).coerceIn(0.0, 100.0) / 100.0

        val total = ((sfiScore * 0.3) + (geoScore * 0.3) + (mufFactor * 40.0)) * absorptionFactor + greylineBonus + activityBonus
        return total.toInt().coerceIn(5, 100)
    }

    override fun getAtmosphericNoise(solarElevation: Double): Double = 30.0
}

class HighBandModel(override val bandName: String, override val centerFreq: Double) : BandModel {
    override fun calculateScore(solarData: SolarData, muf: Double, solarElevation: Double, isGreyline: Boolean, activityReports: Int): Int {
        // 10m/12m/15m/17m - Highly dependent on MUF and SFI
        val mufFactor = calculateMufFactor(centerFreq, muf)
        if (mufFactor < 0.1 && bandName != "17m") return (activityReports / 5).coerceIn(5, 15) // Basically closed without E-skip or high MUF
        
        val sfiScore = ((solarData.solarFlux - 70.0) / 1.2).coerceIn(0.0, 100.0)
        val geoScore = (100.0 - (solarData.kIndex * 10.0)).coerceIn(0.0, 100.0)
        
        // High bands love the sun
        val elevationBonus = (solarElevation / 90.0 * 20.0).coerceAtLeast(0.0)
        val activityBonus = (activityReports.toDouble() / 50.0).coerceAtMost(20.0)

        val total = (mufFactor * 60.0) + (sfiScore * 0.2) + (geoScore * 0.2) + elevationBonus + activityBonus
        return total.toInt().coerceIn(5, 100)
    }

    override fun getAtmosphericNoise(solarElevation: Double): Double = 15.0
}

class VHFBandModel(override val bandName: String, override val centerFreq: Double) : BandModel {
    override fun calculateScore(solarData: SolarData, muf: Double, solarElevation: Double, isGreyline: Boolean, activityReports: Int): Int {
        // 6m - The "Magic Band"
        val mufFactor = calculateMufFactor(centerFreq, muf)
        
        // Massive bonus for activity (Sporadic-E indicator)
        val activityScore = (activityReports.toDouble() / 5.0).coerceIn(0.0, 90.0)
        
        val base = (mufFactor * 100.0).coerceAtLeast(activityScore)
        val geoPenalty = solarData.kIndex * 5.0
        
        return (base - geoPenalty).toInt().coerceIn(5, 100)
    }

    override fun getAtmosphericNoise(solarElevation: Double): Double = 5.0
}
