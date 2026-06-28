package au.com.benji.robert.repository.shack

import au.com.benji.robert.database.ShackEntity
import kotlinx.serialization.Serializable

@Serializable
data class RadioCapabilities(
    val id: Long = 0,
    val model: String,
    val bands: List<String>, // e.g. ["160m", "80m", "40m", "20m", "15m", "10m"] or ["2m", "70cm"]
    val modes: List<String> = listOf("SSB", "CW", "FT8")
)

fun ShackEntity.toRadioCapabilities(): RadioCapabilities? {
    if (category != "Radio") return null
    
    // Simple logic to infer bands from common models for demonstration
    val inferredBands = when {
        model.contains("207", ignoreCase = true) -> listOf("2m", "70cm")
        model.contains("920", ignoreCase = true) -> listOf("160m", "80m", "40m", "30m", "20m", "17m", "15m", "12m", "10m")
        model.contains("IC", ignoreCase = true) && model.contains("705", ignoreCase = true) -> 
            listOf("160m", "80m", "40m", "20m", "15m", "10m", "6m", "2m", "70cm")
        else -> listOf("40m", "20m", "10m") // Default HF
    }
    
    return RadioCapabilities(
        id = id,
        model = model,
        bands = inferredBands
    )
}
