package au.com.benji.robert.repository.propagation

import kotlinx.serialization.Serializable

@Serializable
data class BandCondition(
    val band: String,
    val rating: String, // Poor, Fair, Good, Excellent
    val trend: String // Improving, Stable, Declining
)

@Serializable
data class DuctingAlert(
    val isActive: Boolean,
    val description: String,
    val region: String,
    val intensity: String // None, Low, Moderate, High, Extreme
)

@Serializable
data class PropagationData(
    val bands: List<BandCondition>,
    val ducting: DuctingAlert,
    val timestamp: Long = System.currentTimeMillis()
)
