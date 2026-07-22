package au.com.benji.robert.repository.propagation

import android.util.Log
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.utils.SolarCalculations
import java.util.Locale
import kotlin.math.roundToInt

object PropagationEngine {
    private const val TAG = "PropagationEngine"

    private val bandModels = mapOf(
        "160m" to LowBandModel("160m", 1.8),
        "80m" to LowBandModel("80m", 3.5),
        "40m" to MidBandModel("40m", 7.1),
        "30m" to MidBandModel("30m", 10.1),
        "20m" to MidBandModel("20m", 14.2),
        "17m" to HighBandModel("17m", 18.1),
        "15m" to HighBandModel("15m", 21.2),
        "12m" to HighBandModel("12m", 24.9),
        "10m" to HighBandModel("10m", 28.5),
        "6m" to VHFBandModel("6m", 50.1)
    )

    data class EngineInput(
        val solarData: SolarData,
        val muf: Double,
        val lat: Double,
        val lon: Double,
        val pskReports: Map<String, Int> = emptyMap() // Band name -> report count
    )

    fun calculate(input: EngineInput): PropagationData {
        val solarElevation = SolarCalculations.getSolarElevation(input.lat, input.lon)
        val isGreyline = SolarCalculations.isGreyline(input.lat, input.lon)
        
        Log.d(TAG, "--- New Modular Engine Calculation ---")
        Log.d(TAG, "Solar Elevation: ${String.format("%.2f", solarElevation)}°, Greyline: $isGreyline")

        val bands = bandModels.map { (name, model) ->
            val activity = input.pskReports[name] ?: 0
            val score = model.calculateScore(
                input.solarData, 
                input.muf, 
                solarElevation, 
                isGreyline, 
                activity
            )
            
            val noise = model.getAtmosphericNoise(solarElevation)
            
            BandCondition(
                band = name,
                rating = getRatingLabel(score),
                trend = "Modular",
                score = score,
                color = getRatingColor(score),
                summaries = generateSummaries(name, score, noise, isGreyline)
            )
        }

        // 13. Confidence Score calculation
        val confidence = calculateConfidence(input)

        return PropagationData(
            bands = bands.sortedBy { bandModels[it.band]?.centerFreq ?: 0.0 },
            ducting = DuctingAlert(false, "Scanning atmospheric layers", "Global", "None"),
            aurora = calculateAurora(input.solarData, solarElevation),
            eSkip = calculateESkip(input.solarData, input.muf, input.pskReports["6m"] ?: 0),
            timestamp = System.currentTimeMillis(),
            confidence = confidence
        )
    }

    private fun getRatingLabel(score: Int): String = when {
        score >= 90 -> "Excellent"
        score >= 75 -> "Very Good"
        score >= 60 -> "Good"
        score >= 40 -> "Fair"
        score >= 20 -> "Poor"
        else -> "Closed"
    }

    private fun getRatingColor(score: Int): String = when {
        score >= 90 -> "#2196F3" // Blue for Excellent
        score >= 75 -> "#4CAF50" // Green for Very Good
        score >= 60 -> "#CDDC39" // Lime for Good
        score >= 40 -> "#FFEB3B" // Yellow for Fair
        score >= 20 -> "#FF9800" // Orange for Poor
        else -> "#F44336" // Red for Closed
    }

    private fun calculateConfidence(input: EngineInput): Int {
        var confidence = 95
        
        // Data freshness check
        if (input.muf == 0.0) confidence -= 30
        if (input.pskReports.isEmpty()) confidence -= 5
        
        return confidence.coerceIn(10, 100)
    }

    private fun generateSummaries(band: String, score: Int, noise: Double, isGreyline: Boolean): List<OperatingSummary> {
        val summaries = mutableListOf<OperatingSummary>()
        
        // Dynamic labels based on performance
        val dxRating = if (score > 70) "Excellent" else if (score > 40) "Possible" else "Difficult"
        summaries.add(OperatingSummary("🌍 Long-haul DX", dxRating))
        
        if (isGreyline) {
            summaries.add(OperatingSummary("🌅 Greyline Enhancement", "Active"))
        }

        val noiseLabel = if (noise > 60) "High" else if (noise > 30) "Moderate" else "Low"
        summaries.add(OperatingSummary("🔊 Background Noise", noiseLabel))

        return summaries
    }

    private fun calculateAurora(solarData: SolarData, solarElevation: Double): AuroraReport {
        val k = solarData.kIndex.toDouble()
        val score = when {
            k < 4.0 -> 10
            k < 5.0 -> 30
            k < 6.0 -> 60
            k < 7.0 -> 80
            else -> 100
        }
        val status = if (score > 70) "High" else if (score > 40) "Moderate" else "None"
        return AuroraReport(score, status, if (score > 40) "#4CAF50" else "#9E9E9E", "Auroral activity based on K=$k")
    }

    private fun calculateESkip(solarData: SolarData, muf: Double, reports: Int): ESkipReport {
        val score = if (muf > 40.0 || reports > 50) 70 else 10
        val status = if (score > 60) "Moderate" else "None"
        return ESkipReport(score, status, if (score > 60) "#FF9800" else "#9E9E9E", "Sporadic-E potential")
    }
}
