package au.com.benji.robert.repository.propagation

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class PropagationSpot(
    val id: String,
    val senderCallsign: String,
    val senderLocator: String,
    val senderLat: Double,
    val senderLon: Double,
    val receiverCallsign: String,
    val receiverLocator: String,
    val receiverLat: Double,
    val receiverLon: Double,
    val band: String,
    val mode: String,
    val timestamp: Long,
    val distance: Double,
    val bearing: Double,
    val snr: Int? = null,
    val provider: String
)

interface PropagationProvider {
    val name: String
    suspend fun fetchSpots(
        band: String,
        mode: String,
        timeWindowMinutes: Int
    ): List<PropagationSpot>
}

data class PropagationState(
    val spots: List<PropagationSpot> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdate: Long = 0,
    val activeProviders: List<String> = emptyList()
)
