package au.com.benji.robert.network

import kotlinx.serialization.Serializable

@Serializable
data class SolarFluxEntry(
    val time_tag: String,
    val flux: Double
)

@Serializable
data class PlanetaryKIndex(
    val time_tag: String,
    val k_index: Double,
    val a_index: Double
)
