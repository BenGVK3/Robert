package au.com.benji.robert.models

data class SolarData(
    val solarFlux: Int = 0,
    val kIndex: Int = 0,
    val aIndex: Int = 0,
    val sunspots: Int = 0,
    val muf: String = "---",
    val xRay: String = "---",
    val solarWind: String = "---",
    val protonFlux: String = "---",
    val electronFlux: String = "---",
    val aurora: String = "---",
    val magneticField: String = "---",
    val foF2: String = "---",
    val hfConditionsDay: Map<String, String> = emptyMap(),
    val hfConditionsNight: Map<String, String> = emptyMap(),
    val vhfAurora: String = "---",
    val eSkip: String = "---",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val mufReported: String?
        get() = if (muf != "---" && muf != "NoRpt") muf else null
}
