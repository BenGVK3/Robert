package au.com.benji.robert.models

import kotlinx.serialization.Serializable

@Serializable
data class DxSpot(
    val callsign: String,
    val frequency: String,
    val mode: String = "",
    val spotter: String = "",
    val time: String = "",
    val comment: String = "",
    val source: SpotSource = SpotSource.DX_CLUSTER,
    val location: String = ""
)

enum class SpotSource {
    DX_CLUSTER,
    POTA,
    SOTA,
    WWFF
}
