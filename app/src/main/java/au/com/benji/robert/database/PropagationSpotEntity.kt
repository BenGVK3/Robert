package au.com.benji.robert.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import au.com.benji.robert.repository.propagation.PropagationSpot

@Entity(tableName = "propagation_spots")
data class PropagationSpotEntity(
    @PrimaryKey val id: String,
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
    val snr: Int?,
    val provider: String
) {
    fun toDomainModel(): PropagationSpot {
        return PropagationSpot(
            id = id,
            senderCallsign = senderCallsign,
            senderLocator = senderLocator,
            senderLat = senderLat,
            senderLon = senderLon,
            receiverCallsign = receiverCallsign,
            receiverLocator = receiverLocator,
            receiverLat = receiverLat,
            receiverLon = receiverLon,
            band = band,
            mode = mode,
            timestamp = timestamp,
            distance = distance,
            bearing = bearing,
            snr = snr,
            provider = provider
        )
    }

    companion object {
        fun fromDomainModel(spot: PropagationSpot): PropagationSpotEntity {
            return PropagationSpotEntity(
                id = spot.id,
                senderCallsign = spot.senderCallsign,
                senderLocator = spot.senderLocator,
                senderLat = spot.senderLat,
                senderLon = spot.senderLon,
                receiverCallsign = spot.receiverCallsign,
                receiverLocator = spot.receiverLocator,
                receiverLat = spot.receiverLat,
                receiverLon = spot.receiverLon,
                band = spot.band,
                mode = spot.mode,
                timestamp = spot.timestamp,
                distance = spot.distance,
                bearing = spot.bearing,
                snr = spot.snr,
                provider = spot.provider
            )
        }
    }
}
