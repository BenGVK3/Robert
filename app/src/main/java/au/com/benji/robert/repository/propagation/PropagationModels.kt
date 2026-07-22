package au.com.benji.robert.repository.propagation

import kotlinx.serialization.Serializable

@Serializable
data class BandCondition(
    val band: String,
    val rating: String, // Poor, Fair, Good, Excellent
    val trend: String, // Improving, Stable, Declining
    val score: Int = 0,
    val color: String = "#CCCCCC",
    val history: List<Int> = emptyList(),
    val historicalData: List<PropagationPoint> = emptyList(), // Real history
    val forecastData: List<PropagationPoint> = emptyList(), // Predictions
    val summaries: List<OperatingSummary> = emptyList()
)

@Serializable
data class PropagationPoint(
    val timestamp: Long,
    val score: Int
)

@Serializable
data class OperatingSummary(
    val label: String,
    val rating: String, // Poor, Fair, Good, Excellent
    val icon: String = "" // Optional icon or emoji
)

@Serializable
data class DuctingAlert(
    val isActive: Boolean,
    val description: String,
    val region: String,
    val intensity: String // None, Low, Moderate, High, Extreme
)

@Serializable
data class AuroraReport(
    val score: Int,
    val status: String, // None, Very Low, Low, Moderate, High, Extreme
    val color: String,
    val description: String
)

@Serializable
data class ESkipReport(
    val score: Int,
    val status: String,
    val color: String,
    val description: String
)

@Serializable
data class PropagationData(
    val bands: List<BandCondition>,
    val ducting: DuctingAlert,
    val aurora: AuroraReport? = null,
    val eSkip: ESkipReport? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Int = 90
)
