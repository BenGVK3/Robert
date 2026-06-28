package au.com.benji.robert.repository

import au.com.benji.robert.models.SolarData

class SolarRepository {

    fun getCurrentConditions(): SolarData {

        return SolarData(
            solarFlux = 128,
            kIndex = 2.0,
            aIndex = 8,
            sunspots = 0,
            muf = "31 MHz"
        )
    }
}