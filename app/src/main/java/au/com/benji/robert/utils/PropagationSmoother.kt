package au.com.benji.robert.utils

import au.com.benji.robert.repository.propagation.PropagationPoint
import kotlin.math.abs

object PropagationSmoother {

    /**
     * Applies Exponential Moving Average (EMA) and Rate Limiting.
     * Each band has its own constraints for realism.
     */
    fun smoothHistory(band: String, rawPoints: List<PropagationPoint>): List<PropagationPoint> {
        if (rawPoints.isEmpty()) return emptyList()
        
        val alpha = getBandAlpha(band)
        val maxChange = getBandMaxChange(band)
        
        val smoothed = mutableListOf<PropagationPoint>()
        var lastValue = rawPoints.first().score.toDouble()
        
        smoothed.add(rawPoints.first())

        for (i in 1 until rawPoints.size) {
            val currentRaw = rawPoints[i].score.toDouble()
            
            // 1. Exponential Smoothing
            var targetValue = (alpha * currentRaw) + ((1.0 - alpha) * lastValue)
            
            // 2. Rate Limiting (unless exceptional event)
            val diff = targetValue - lastValue
            val isExceptional = abs(currentRaw - lastValue) > 40.0 // Detect solar flare / storm
            
            if (!isExceptional && abs(diff) > maxChange) {
                targetValue = if (diff > 0) lastValue + maxChange else lastValue - maxChange
            }
            
            // 3. Natural Micro-Noise (0.5% variation)
            val noise = (Math.random() - 0.5) * 0.5
            targetValue += noise

            lastValue = targetValue
            smoothed.add(rawPoints[i].copy(score = targetValue.toInt().coerceIn(0, 100)))
        }
        
        return smoothed
    }

    private fun getBandAlpha(band: String): Double = when (band) {
        "160m", "80m" -> 0.15 // Very slow
        "40m", "20m" -> 0.25 // Standard
        "15m", "10m" -> 0.4  // Faster
        "6m" -> 0.6          // Very reactive
        else -> 0.3
    }

    private fun getBandMaxChange(band: String): Double = when (band) {
        "160m", "80m" -> 5.0
        "40m", "20m" -> 8.0
        "15m", "10m" -> 12.0
        "6m" -> 25.0 // Allow rapid spikes for Sporadic-E
        else -> 10.0
    }
}
