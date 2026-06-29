package au.com.benji.robert.repository

import android.util.Log
import android.util.Xml
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class SolarDataRepository {
    private val TAG = "SolarDataRepository"
    private var cachedData: SolarData? = null

    fun getSolarData(lat: Double? = null, lon: Double? = null): Flow<SolarData> = flow {
        while (true) {
            try {
                Log.d(TAG, "Fetching solar data from HamQSL...")
                val xmlContent = ApiService.fetchData("https://www.hamqsl.com/solarxml.php")
                
                if (xmlContent != null) {
                    val parsedData = parseSolarXml(xmlContent)
                    cachedData = parsedData
                    emit(parsedData)
                    delay(15 * 60 * 1000) // 15 minutes
                } else {
                    Log.w(TAG, "Fetch failed, using cached data if available")
                    cachedData?.let { emit(it) }
                    delay(30 * 1000) // Retry in 30 seconds
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in SolarDataRepository: ${e.message}")
                cachedData?.let { emit(it) }
                delay(30 * 1000) // Retry in 30 seconds
            }
        }
    }

    private suspend fun parseSolarXml(xml: String): SolarData = withContext(Dispatchers.Default) {
        val data = mutableMapOf<String, String>()
        val hfDay = mutableMapOf<String, String>()
        val hfNight = mutableMapOf<String, String>()
        
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var eventType = parser.eventType
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "band") {
                            val name = parser.getAttributeValue(null, "name")
                            val time = parser.getAttributeValue(null, "time")
                            val condition = parser.nextText()
                            if (time == "day") hfDay[name] = condition
                            else if (time == "night") hfNight[name] = condition
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty()) {
                            data[currentTag] = text
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "XML Parsing error: ${e.message}")
        }

        SolarData(
            solarFlux = data["solarflux"]?.toIntOrNull() ?: 0,
            sunspots = data["sunspots"]?.toIntOrNull() ?: 0,
            aIndex = data["aindex"]?.toIntOrNull() ?: 0,
            kIndex = data["kindex"]?.toIntOrNull() ?: 0,
            xRay = data["xray"] ?: "---",
            solarWind = data["solarwind"] ?: "---",
            protonFlux = data["protonflux"] ?: "---",
            electronFlux = data["electronflux"] ?: "---",
            aurora = data["aurora"] ?: "---",
            magneticField = data["magneticfield"] ?: "---",
            foF2 = data["fof2"] ?: "---",
            muf = data["muf"] ?: "---",
            hfConditionsDay = hfDay,
            hfConditionsNight = hfNight,
            vhfAurora = data["vhfaurora"] ?: "---",
            eSkip = data["eskip"] ?: "---",
            lastUpdated = System.currentTimeMillis()
        )
    }
}
