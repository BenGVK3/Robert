package au.com.benji.robert.repository.dx

import au.com.benji.robert.models.DxSpot

interface DxSpotProvider {
    val name: String
    suspend fun fetchSpots(): List<DxSpot>
}
