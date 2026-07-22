package au.com.benji.robert.network

import android.content.Context
import android.util.Log
import android.util.Xml
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.database.SolarDataEntity
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.utils.MufCalculator
import au.com.benji.robert.repository.propagation.PropagationEngine
import au.com.benji.robert.database.PropagationHistoryEntity
import au.com.benji.robert.utils.SolarCalculations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class SolarWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val TAG = "SolarWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Background Solar Data Fetch Starting...")
            val xmlContent = ApiService.fetchData("https://www.hamqsl.com/solarxml.php")
            
            if (xmlContent != null) {
                val parsedData = parseSolarXml(xmlContent)
                val cacheDao = DatabaseModule.cacheDao(applicationContext)
                cacheDao.insertSolar(parsedData.toEntity())
                
                // Also update propagation history for background graphs
                updatePropagationHistory(parsedData)
                
                Log.d(TAG, "Background Solar Data Sync Successful")
                Result.success()
            } else {
                Log.w(TAG, "Background Solar Data Fetch Failed (Empty Content)")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Background Solar Data Error: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun updatePropagationHistory(solar: SolarData) {
        val cacheDao = DatabaseModule.cacheDao(applicationContext)
        val settings = cacheDao.getSettingsSync() ?: return
        val lat = settings.homeLat
        val lon = settings.homeLon
        
        if (lat == 0.0 && lon == 0.0) return

        val mufResult = MufCalculator.calculateMuf(solar, lat, lon)
        val input = PropagationEngine.EngineInput(
            solarData = solar,
            muf = mufResult.value,
            lat = lat,
            lon = lon
        )
        
        val currentData = PropagationEngine.calculate(input)
        val propagationDao = DatabaseModule.propagationDao(applicationContext)
        val now = System.currentTimeMillis()

        currentData.bands.forEach { band ->
            propagationDao.insert(
                PropagationHistoryEntity(
                    timestamp = now,
                    band = band.band,
                    score = band.score,
                    muf = mufResult.value,
                    sfi = solar.solarFlux,
                    kIndex = solar.kIndex,
                    aIndex = solar.aIndex,
                    solarElevation = SolarCalculations.getSolarElevation(lat, lon),
                    confidence = currentData.confidence
                )
            )
        }
    }

    private fun parseSolarXml(xml: String): SolarData {
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

        return SolarData(
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
}
