package au.com.benji.robert.repository.propagation

import au.com.benji.robert.models.SolarData
import kotlin.math.*

/**
 * Common interface for band-specific propagation logic.
 */
interface BandModel {
    val bandName: String
    val centerFreq: Double // MHz
    
    fun calculateScore(
        solarData: SolarData,
        muf: Double,
        solarElevation: Double,
        isGreyline: Boolean,
        activityReports: Int
    ): Int

    fun getAtmosphericNoise(solarElevation: Double): Double
}

/**
 * Base logic for calculating D-Layer absorption dynamically.
 */
fun calculateDLayerAbsorption(freq: Double, solarElevation: Double, sfi: Int): Double {
    if (solarElevation <= -10.0) return 0.0 // No absorption at night
    
    // Empirical D-layer model: higher SFI and higher sun elevation = more absorption.
    // Absorption decreases with the square of frequency (1/f^2).
    val elevationFactor = max(0.0, sin(Math.toRadians(solarElevation)))
    val sfiFactor = (sfi.toDouble() / 100.0).pow(0.5)
    val absorption = (sfiFactor * elevationFactor * 500.0) / freq.pow(2.0)
    
    return absorption.coerceIn(0.0, 100.0)
}

/**
 * Logistic Sigmoid for MUF transitions.
 */
fun calculateMufFactor(freq: Double, muf: Double): Double {
    val ratio = muf / freq
    // Transition centered at 0.95 of MUF with a steepness of 15
    return (1.0 / (1.0 + exp(-15.0 * (ratio - 0.95))))
}
