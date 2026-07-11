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
                    if (parts.any { it.contains("Call", ignoreCase = true) || it.contains("Freq", ignoreCase = true) }) {
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
        
        Log.d(TAG, "Parsed ${repeaters.size} repeaters")
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
        
        val data = mutableMapOf<String, String>()
        header.forEachIndexed { index, name ->
            val value = parts.getOrNull(index)?.trim() ?: ""
            data[name] = value
        }
        
        fun find(vararg keys: String): String? {
            for (key in keys) {
                val k = key.lowercase()
                if (data.containsKey(k)) return data[k]
                val actualKey = data.keys.find { it.contains(k) }
                if (actualKey != null) return data[actualKey]
            }
            return null
        }

        val callsign = find("callsign", "call sign", "call", "stn call") ?: ""
        if (callsign.isEmpty() || callsign.lowercase() == "callsign" || callsign.lowercase() == "call") return null

        val frequency = find("output", "freq (mhz)", "frequency", "freq", "output (mhz)") ?: ""
        if (frequency.isEmpty()) return null
        
        val lat = find("lat", "latitude")?.toDoubleOrNull() ?: 0.0
        val lng = find("long", "longitude", "lng")?.toDoubleOrNull() ?: 0.0
        
        val offset = find("offset", "shift", "offset (mhz)") ?: ""
        
        var inputFreq = find("input", "input (mhz)", "input freq") ?: ""
        if (inputFreq.isEmpty() && offset.isNotEmpty()) {
            try {
                val freqNum = frequency.toDouble()
                val offsetNum = offset.toDoubleOrNull() ?: 0.0
                inputFreq = (freqNum + offsetNum).toString()
            } catch (e: Exception) { }
        }

        // Improved state detection
        var state = find("state", "st", "territory", "region")
        if (state.isNullOrBlank()) {
            // Fallback: Infer state from callsign if it's Australian
            state = inferStateFromCallsign(callsign)
        }

        return RepeaterEntity(
            callsign = callsign.uppercase(),
            frequency = frequency,
            name = find("name", "repeater name", "site name", "repeater", "description"),
            inputFreq = if (inputFreq.isNotBlank()) inputFreq else null,
            offset = offset,
            band = find("band", "band (m)", "frequency band"),
            mode = find("mode", "digital mode", "emission", "type"),
            tone = find("ctcss", "tone", "access", "tone/access", "subtone"),
            dcs = find("dcs", "dtcs"),
            location = find("location", "site", "site name"),
            town = find("town", "locality", "city", "suburb"),
            state = state,
            lat = lat,
            lng = lng,
            gridSquare = find("grid square", "grid") ?: calculateMaidenhead(lat, lng),
            elevation = find("asl", "elevation", "height", "ht"),
            notes = find("notes", "sota/pota", "comments", "remarks", "info"),
            status = find("status", "condition", "state"),
            lastUpdate = find("last updated", "updated", "date", "revision")
        )
    }

    private fun inferStateFromCallsign(callsign: String): String? {
        val upper = callsign.uppercase()
        return when {
            upper.startsWith("VK1") -> "ACT"
            upper.startsWith("VK2") -> "NSW"
            upper.startsWith("VK3") -> "VIC"
            upper.startsWith("VK4") -> "QLD"
            upper.startsWith("VK5") -> "SA"
            upper.startsWith("VK6") -> "WA"
            upper.startsWith("VK7") -> "TAS"
            upper.startsWith("VK8") -> "NT"
            upper.startsWith("VK9") -> "External Territory"
            upper.startsWith("VK0") -> "Antarctica"
            else -> null
        }
    }
}
