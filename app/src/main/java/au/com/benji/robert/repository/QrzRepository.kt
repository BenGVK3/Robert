package au.com.benji.robert.repository

import android.util.Log
import au.com.benji.robert.database.CacheDao
import au.com.benji.robert.database.CallsignCacheEntity
import au.com.benji.robert.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QrzRepository(private val cacheDao: CacheDao) {
    private var sessionKey: String? = null

    suspend fun login(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        if (username.isEmpty() || password.isEmpty()) return@withContext false
        val url = "https://xmldata.qrz.com/xml/current/?username=$username;password=$password;agent=RobertApp"
        val response = ApiService.fetchData(url)
        sessionKey = response?.let { extractValue(it, "Key") }
        sessionKey != null
    }

    suspend fun lookup(callsign: String): QrzData? = withContext(Dispatchers.IO) {
        // 1. Check Cache
        val cached = cacheDao.getCallsign(callsign)
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < 7 * 24 * 60 * 60 * 1000) {
            return@withContext cached.toModel()
        }

        // 2. Fetch Network
        val key = sessionKey ?: return@withContext null
        val url = "https://xmldata.qrz.com/xml/current/?s=$key;callsign=$callsign"
        val response = ApiService.fetchData(url) ?: return@withContext null
        
        if (response.contains("Session Timeout") || response.contains("Invalid session key")) {
            sessionKey = null
            return@withContext null
        }

        val data = QrzData(
            callsign = extractValue(response, "call") ?: callsign,
            name = "${extractValue(response, "fname") ?: ""} ${extractValue(response, "name") ?: ""}".trim(),
            qth = "${extractValue(response, "addr2") ?: ""}, ${extractValue(response, "state") ?: ""} ${extractValue(response, "country") ?: ""}".trim(),
            grid = extractValue(response, "grid") ?: "---",
            class_ = extractValue(response, "class") ?: "---",
            qslCount = extractValue(response, "qslmgr") ?: "None",
            image = extractValue(response, "image") ?: ""
        )

        // 3. Update Cache
        cacheDao.insertCallsign(data.toEntity())
        data
    }

    private fun extractValue(xml: String, tag: String): String? {
        val startTag = "<$tag>"
        val endTag = "</$tag>"
        val start = xml.indexOf(startTag)
        if (start == -1) return null
        val end = xml.indexOf(endTag, start)
        if (end == -1) return null
        return xml.substring(start + startTag.length, end)
    }

    private fun QrzData.toEntity() = CallsignCacheEntity(
        callsign = callsign,
        name = name,
        qth = qth,
        grid = grid,
        licenceClass = class_,
        imageUrl = image
    )

    private fun CallsignCacheEntity.toModel() = QrzData(
        callsign = callsign,
        name = name,
        qth = qth,
        grid = grid,
        class_ = licenceClass,
        qslCount = "None",
        image = imageUrl
    )
}

data class QrzData(
    val callsign: String,
    val name: String,
    val qth: String,
    val grid: String,
    val class_: String,
    val qslCount: String,
    val image: String
)
