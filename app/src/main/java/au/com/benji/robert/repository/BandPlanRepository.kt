package au.com.benji.robert.repository

import android.content.Context
import au.com.benji.robert.models.BandPlan
import kotlinx.serialization.json.Json
import java.io.IOException

class BandPlanRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun getBandPlan(country: String): BandPlan? {
        val fileName = when (country) {
            "Australia" -> "au.json"
            "United States" -> "us.json"
            "Canada" -> "ca.json"
            "United Kingdom" -> "uk.json"
            "New Zealand" -> "nz.json"
            "Japan" -> "jp.json"
            "IARU Region 1" -> "iaru_region1.json"
            "IARU Region 2" -> "iaru_region2.json"
            "IARU Region 3" -> "iaru_region3.json"
            else -> "au.json"
        }

        return try {
            val jsonString = context.assets.open("bandplans/$fileName").bufferedReader().use { it.readText() }
            json.decodeFromString<BandPlan>(jsonString)
        } catch (e: IOException) {
            null
        }
    }
    
    fun getSupportedCountries(): List<String> {
        return listOf(
            "Australia", "United States", "Canada", "United Kingdom",
            "New Zealand", "Japan", "IARU Region 1", "IARU Region 2", "IARU Region 3"
        )
    }
}
