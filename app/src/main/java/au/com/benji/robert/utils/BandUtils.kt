package au.com.benji.robert.utils

object BandUtils {
    data class BandAllocation(
        val name: String,
        val minFreqKhz: Double,
        val maxFreqKhz: Double,
        val primaryModes: List<String> = emptyList()
    )

    private val allocations = listOf(
        BandAllocation("2200m", 135.7, 137.8),
        BandAllocation("630m", 472.0, 479.0),
        BandAllocation("160m", 1800.0, 2000.0),
        BandAllocation("80m", 3500.0, 4000.0),
        BandAllocation("60m", 5330.5, 5406.5),
        BandAllocation("40m", 7000.0, 7300.0),
        BandAllocation("30m", 10100.0, 10150.0),
        BandAllocation("20m", 14000.0, 14350.0),
        BandAllocation("17m", 18068.0, 18168.0),
        BandAllocation("15m", 21000.0, 21450.0),
        BandAllocation("12m", 24890.0, 24990.0),
        BandAllocation("10m", 28000.0, 29700.0),
        BandAllocation("6m", 50000.0, 54000.0),
        BandAllocation("2m", 144000.0, 148000.0),
        BandAllocation("70cm", 420000.0, 450000.0),
        BandAllocation("23cm", 1240000.0, 1300000.0)
    )

    fun getBandFromFrequency(frequencyKhz: Double): String {
        return allocations.find { frequencyKhz in it.minFreqKhz..it.maxFreqKhz }?.name ?: ""
    }

    fun isWithinAmateurBand(frequencyKhz: Double): Boolean {
        return allocations.any { frequencyKhz in it.minFreqKhz..it.maxFreqKhz }
    }

    fun getSuggestedMode(frequencyKhz: Double): String {
        val band = allocations.find { frequencyKhz in it.minFreqKhz..it.maxFreqKhz } ?: return ""
        
        // General rule: Bottom of band is CW, then Digital, then Phone/SSB
        // This is a simplified version of standard band plans
        val bandRange = band.maxFreqKhz - band.minFreqKhz
        val offset = frequencyKhz - band.minFreqKhz
        
        return when {
            offset < bandRange * 0.1 -> "CW"
            offset < bandRange * 0.3 -> "DIGI"
            else -> "SSB"
        }
    }
}
