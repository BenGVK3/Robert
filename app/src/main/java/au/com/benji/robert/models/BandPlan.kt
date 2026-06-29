package au.com.benji.robert.models

import kotlinx.serialization.Serializable

@Serializable
data class BandPlan(
    val country: String,
    val countryCode: String,
    val licenceClasses: List<LicenceClass>,
    val bands: List<Band>
)

@Serializable
data class LicenceClass(
    val name: String,
    val id: String
)

@Serializable
data class Band(
    val name: String,
    val frequencyRange: FrequencyRange,
    val allocations: List<BandAllocation>,
    val notes: String? = null
)

@Serializable
data class FrequencyRange(
    val start: Double, // In MHz
    val end: Double    // In MHz
)

@Serializable
data class BandAllocation(
    val name: String,
    val frequencyRange: FrequencyRange,
    val modes: List<String>,
    val licenceRestrictions: Map<String, RestrictionType>, // licenceId -> RestrictionType
    val powerLimits: Map<String, String>? = null, // licenceId -> Power Limit string
    val description: String? = null
)

enum class RestrictionType {
    ALLOWED,
    RESTRICTED,
    NOT_PERMITTED
}
