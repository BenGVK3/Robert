package au.com.benji.robert.repository.propagation

import android.util.Log
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.utils.PropagationCalculator
import java.util.Locale

object PropagationEngine {
    private const val TAG = "PropagationEngine"

    data class EngineInput(
        val solarData: SolarData,
        val muf: Double,
        val lat: Double,
        val lon: Double,
        val sunrise: String? = null,
        val sunset: String? = null,
        val psk6m: Int = 0,
        val psk10m: Int = 0
    )

    fun calculate(input: EngineInput): PropagationData {
        Log.d(TAG, "--- Propagation Calculation Start ---")
        Log.d(TAG, "Data Source: Real-time NOAA/HamQSL")
        Log.d(TAG, "Solar Inputs: SFI=${input.solarData.solarFlux}, K=${input.solarData.kIndex}, A=${input.solarData.aIndex}")
        Log.d(TAG, "X-Ray: ${input.solarData.xRay}, Wind: ${input.solarData.solarWind}")
        Log.d(TAG, "MUF: ${String.format(Locale.US, "%.2f", input.muf)} MHz")
        Log.d(TAG, "Location: ${input.lat}, ${input.lon}")
        Log.d(TAG, "Traffic Density: 6m=${input.psk6m}, 10m=${input.psk10m}")

        val bandScores = PropagationCalculator.calculateAllBands(
            solarData = input.solarData,
            muf = input.muf,
            lat = input.lat,
            lon = input.lon,
            sunrise = input.sunrise,
            sunset = input.sunset
        )

        val bands = bandScores.map { score ->
            Log.d(TAG, "Result -> Band ${score.band.padEnd(4)}: Score=${score.score.toString().padStart(3)}, Status=${score.rating}")
            BandCondition(
                band = score.band,
                rating = score.rating,
                trend = "Stable", 
                score = score.score,
                color = score.colorHex,
                summaries = score.summaries
            )
        }

        val aurora = PropagationCalculator.calculateAurora(input.solarData)
        val eSkip = PropagationCalculator.calculateESkip(input.solarData, input.muf, input.psk6m, input.psk10m)

        Log.d(TAG, "Aurora Status: ${aurora.status} (Score: ${aurora.score})")
        Log.d(TAG, "E-Skip Status: ${eSkip.status} (Score: ${eSkip.score})")
        Log.d(TAG, "--- Propagation Calculation End ---")

        return PropagationData(
            bands = bands,
            ducting = DuctingAlert(false, "No active alerts", "Global", "None"),
            aurora = aurora,
            eSkip = eSkip,
            timestamp = System.currentTimeMillis()
        )
    }
}
