package au.com.benji.robert.repository

import android.util.Log
import au.com.benji.robert.database.RepeaterEntity
import au.com.benji.robert.utils.calculateMaidenhead
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object WiaCsvParser {
    private const val TAG = "WiaCsvParser"
    
    fun parse(inputStream: InputStream): List<RepeaterEntity> {
        val repeaters = mutableListOf<RepeaterEntity>()
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        
        var header: List<String>? = null
        
        reader.useLines { lines ->
            lines.forEach { line ->
                if (line.isBlank()) return@forEach
                
                val parts = parseCsvLine(line)
                
                if (header == null) {
                    // Look for the header line which usually contains 'Callsign'
                    if (parts.any { it.contains("Callsign", ignoreCase = true) || it.contains("Call", ignoreCase = true) }) {
                        header = parts.map { it.trim().lowercase() }
                        Log.d(TAG, "Found header: $header")
                    }
                } else {
                    try {
                        val repeater = mapToEntity(parts, header!!)
                        if (repeater != null) {
                            repeaters.add(repeater)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing line: $line", e)
                    }
                }
            }
        }
        
        return repeaters
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '\"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }
    
    private fun mapToEntity(parts: List<String>, header: List<String>): RepeaterEntity? {
        if (parts.size < 3) return null
        
        val data = header.mapIndexed { index, name -> 
            name to (parts.getOrNull(index) ?: "")
        }.toMap()
        
        // Match columns based on various possible names in WIA CSV
        val callsign = data["callsign"] ?: data["call"] ?: data["call sign"] ?: ""
        if (callsign.isEmpty() || callsign.lowercase() == "callsign") return null

        val frequency = data["freq (mhz)"] ?: data["frequency"] ?: data["output"] ?: data["freq"] ?: ""
        if (frequency.isEmpty()) return null
        
        val lat = data["lat"]?.toDoubleOrNull() ?: data["latitude"]?.toDoubleOrNull() ?: 0.0
        val lng = data["long"]?.toDoubleOrNull() ?: data["longitude"]?.toDoubleOrNull() ?: data["lng"]?.toDoubleOrNull() ?: 0.0
        
        val offset = data["offset (mhz)"] ?: data["offset"] ?: ""
        val inputFreq = data["input"] ?: data["input freq"] ?: try {
            val freqNum = frequency.toDouble()
            val offsetNum = offset.toDoubleOrNull() ?: 0.0
            (freqNum + offsetNum).toString()
        } catch (e: Exception) {
            null
        }

        return RepeaterEntity(
            callsign = callsign.uppercase(),
            frequency = frequency,
            name = data["name"] ?: data["repeater name"] ?: data["site name"],
            inputFreq = inputFreq,
            offset = offset,
            band = data["band"] ?: data["band (m)"],
            mode = data["mode"] ?: data["digital mode"],
            tone = data["ctcss (hz)"] ?: data["ctcss"] ?: data["tone"] ?: data["access"],
            dcs = data["dcs"],
            location = data["location"] ?: data["site"],
            town = data["town"] ?: data["locality"],
            state = data["state"],
            lat = lat,
            lng = lng,
            gridSquare = data["grid square"] ?: data["grid"] ?: calculateMaidenhead(lat, lng),
            elevation = data["asl (m)"] ?: data["elevation"] ?: data["asl"] ?: data["height"],
            notes = data["notes"] ?: data["sota/pota"] ?: data["comments"],
            status = data["status"],
            lastUpdate = data["last updated"] ?: data["updated"] ?: data["date"]
        )
    }
}
