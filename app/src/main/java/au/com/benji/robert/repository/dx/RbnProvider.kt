package au.com.benji.robert.repository.dx

import au.com.benji.robert.models.DxSpot
import au.com.benji.robert.models.SpotSource

class RbnProvider : DxSpotProvider {
    override val name: String = "RBN"

    override suspend fun fetchSpots(): List<DxSpot> {
        // TODO: Implement RBN API integration
        // Currently RBN typically requires Telnet or a specific web-scrape/API if available.
        // Reverse Beacon Network (RBN) provides CW/Digital automated spots.
        return emptyList()
    }
}
