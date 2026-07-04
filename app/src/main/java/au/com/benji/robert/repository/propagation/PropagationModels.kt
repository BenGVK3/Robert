package au.com.benji.robert.repository.propagation

import kotlinx.serialization.Serializable

@Serializable
data class BandCondition(
    val band: String,
    val rating: String, // Poor, Fair, Good, Excellent
    val trend: String, // Improving, Stable, Declining
    val score: Int = 0,
    val color: String = "#CCCCCC",
    val history: List<Int> = emptyList()
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
    val timestamp: Long = System.currentTimeMillis()
)
