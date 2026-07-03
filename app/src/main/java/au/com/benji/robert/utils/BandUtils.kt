package au.com.benji.robert.utils

object BandUtils {
    fun getBandFromFrequency(frequencyKhz: Double): String {
        return when {
            frequencyKhz in 135.7..137.8 -> "2200m"
            frequencyKhz in 472.0..479.0 -> "630m"
            frequencyKhz in 1800.0..2000.0 -> "160m"
            frequencyKhz in 3500.0..4000.0 -> "80m"
            frequencyKhz in 5330.5..5406.5 -> "60m"
            frequencyKhz in 7000.0..7300.0 -> "40m"
            frequencyKhz in 10100.0..10150.0 -> "30m"
            frequencyKhz in 14000.0..14350.0 -> "20m"
            frequencyKhz in 18068.0..18168.0 -> "17m"
            frequencyKhz in 21000.0..21450.0 -> "15m"
            frequencyKhz in 24890.0..24990.0 -> "12m"
            frequencyKhz in 28000.0..29700.0 -> "10m"
            frequencyKhz in 50000.0..54000.0 -> "6m"
            frequencyKhz in 144000.0..148000.0 -> "2m"
            frequencyKhz in 420000.0..450000.0 -> "70cm"
            frequencyKhz in 1240000.0..1300000.0 -> "23cm"
            else -> ""
        }
    }
}
