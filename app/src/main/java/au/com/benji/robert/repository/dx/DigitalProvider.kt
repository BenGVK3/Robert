package au.com.benji.robert.repository.dx

import au.com.benji.robert.models.DxSpot
import au.com.benji.robert.models.SpotSource

class DigitalProvider : DxSpotProvider {
    override val name: String = "Digital"

    override suspend fun fetchSpots(): List<DxSpot> {
        // TODO: Implement PSK Reporter or similar digital spot provider
        // PSK Reporter provides live mapping of digital modes (FT8, FT4 etc)
        return emptyList()
    }
}
