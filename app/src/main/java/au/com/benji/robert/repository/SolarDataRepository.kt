package au.com.benji.robert.repository

import android.util.Log
import android.util.Xml
import au.com.benji.robert.database.CacheDao
import au.com.benji.robert.database.SolarDataEntity
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class SolarDataRepository(private val cacheDao: CacheDao) {
    private val TAG = "SolarDataRepository"

    fun getSolarData(lat: Double? = null, lon: Double? = null): Flow<SolarData> {
        val refreshFlow = flow<Unit?> {
            while (true) {
                try {
                    Log.d(TAG, "Fetching fresh solar data from HamQSL...")
                    val xmlContent = ApiService.fetchData("https://www.hamqsl.com/solarxml.php")
                    
                    if (xmlContent != null) {
                        val parsedData = parseSolarXml(xmlContent)
                        cacheDao.insertSolar(parsedData.toEntity())
                        delay(5 * 60 * 1000)
                    } else {
                        delay(60 * 1000)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in SolarDataRepository refresh: ${e.message}")
                    delay(60 * 1000)
                }
                emit(null)
            }
        }

        return merge(
            cacheDao.getSolarFlow().filterNotNull().map { it.toModel() },
            refreshFlow.filter { false }.map { SolarData() } // Trick to keep refreshFlow running without emitting dummy data
        ).distinctUntilChanged()
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

    private fun SolarData.toEntity() = SolarDataEntity(
        solarFlux = solarFlux,
        kIndex = kIndex,
        aIndex = aIndex,
        sunspots = sunspots,
        muf = muf,
        xRay = xRay,
        solarWind = solarWind,
        protonFlux = protonFlux,
        electronFlux = electronFlux,
        aurora = aurora,
        magneticField = magneticField,
        foF2 = foF2,
        hfConditionsDay = hfConditionsDay,
        hfConditionsNight = hfConditionsNight,
        vhfAurora = vhfAurora,
        eSkip = eSkip,
        lastUpdated = lastUpdated
    )

    private fun SolarDataEntity.toModel() = SolarData(
        solarFlux = solarFlux,
        kIndex = kIndex,
        aIndex = aIndex,
        sunspots = sunspots,
        muf = muf,
        xRay = xRay,
        solarWind = solarWind,
        protonFlux = protonFlux,
        electronFlux = electronFlux,
        aurora = aurora,
        magneticField = magneticField,
        foF2 = foF2,
        hfConditionsDay = hfConditionsDay,
        hfConditionsNight = hfConditionsNight,
        vhfAurora = vhfAurora,
        eSkip = eSkip,
        lastUpdated = lastUpdated
    )
}
