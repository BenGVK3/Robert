package au.com.benji.robert.models

data class SolarData(
    val solarFlux: Int,
    val kIndex: Double,
    val aIndex: Int,
    val sunspots: Int,
    val muf: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
